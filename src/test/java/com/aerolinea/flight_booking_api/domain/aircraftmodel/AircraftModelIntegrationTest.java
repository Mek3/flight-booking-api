package com.aerolinea.flight_booking_api.domain.aircraftmodel;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.aerolinea.flight_booking_api.config.AbstractIntegrationTest;
import com.aerolinea.flight_booking_api.dtos.aircraftmodel.AircraftModelRequest;
import com.aerolinea.flight_booking_api.dtos.aircraftmodel.AircraftModelResponse;
import com.aerolinea.flight_booking_api.models.Role;
import com.aerolinea.flight_booking_api.models.User;
import com.aerolinea.flight_booking_api.services.JwtService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class AircraftModelIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsServiceMock;

    @MockitoBean
    private UserDetails mockUserDetails;

    @Autowired
    private WebTestClient webTestClient;

    private String adminJwtToken;

    @BeforeEach
    void setUp() {
        String username = "system_admin";
        User adminUser = User.builder()
                .name("System")
                .surname("Admin")
                .email("admin@flightapi.com")
                .username(username)
                .password("dummy_password")
                .phone("555-0199")
                .build();

        Role adminRole = Role.builder()
                .name("ROLE_ADMIN")
                .description("System Administrator")
                .build();
        
        adminUser.addRole(adminRole);

        Authentication adminAuth = new UsernamePasswordAuthenticationToken(
                adminUser,
                null,
                adminUser.getAuthorities()
        );

        this.adminJwtToken = jwtService.generateToken(adminAuth);

        when(userDetailsServiceMock.loadUserByUsername(username)).thenReturn(adminUser);
    }

    @Test
    void createAircraftModel_ShouldReturn201_WhenPayloadIsValidAndUserIsAdmin() {
        AircraftModelRequest request = new AircraftModelRequest("Airbus", "A320-Neo-Test1", (short)180);

        webTestClient.post()
            .uri("/api/v1/aircraft-models")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id").isNumber()
            .jsonPath("$.manufacturer").isEqualTo("Airbus")
            .jsonPath("$.modelName").isEqualTo("A320-Neo-Test1");
    }

    @Test
    void createAircraftModel_ShouldReturn400_WhenPayloadIsInvalid() {
        AircraftModelRequest request = new AircraftModelRequest("", "",(short) -10);

        webTestClient.post()
            .uri("/api/v1/aircraft-models")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo(400)
            .jsonPath("$.message").isNotEmpty();
    }

    @Test
    void getAllAircraftModels_ShouldReturn200_WhenQueryParametersAreValid() {
        webTestClient.get()
            .uri("/api/v1/aircraft-models?size=5&sort=manufacturer")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.content").isArray()
            .jsonPath("$.pageable").exists();
    }

    @Test
    void deleteAircraftModel_ShouldReturn204_WhenUserIsAdminAndIdExists() {
        Integer idToDelete = webTestClient.post()
            .uri("/api/v1/aircraft-models")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(new AircraftModelRequest("Boeing", "787-Test-Delete", (short) 250))
            .exchange()
            .expectStatus().isCreated()
            .expectBody(AircraftModelResponse.class)
            .returnResult()
            .getResponseBody()
            .id().intValue();

        webTestClient.delete()
            .uri("/api/v1/aircraft-models/" + idToDelete)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
            .exchange()
            .expectStatus().isNoContent();
            
        webTestClient.get()
            .uri("/api/v1/aircraft-models/" + idToDelete)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
            .exchange()
            .expectStatus().isNotFound();
    }
}