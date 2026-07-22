package com.aerolinea.flight_booking_api.domain.airport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.security.core.Authentication;

import com.aerolinea.flight_booking_api.config.AbstractIntegrationTest;
import com.aerolinea.flight_booking_api.dtos.airport.AirportRequest;
import com.aerolinea.flight_booking_api.models.Role;
import com.aerolinea.flight_booking_api.models.User;
import com.aerolinea.flight_booking_api.services.JwtService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class AirportIntegrationTest extends AbstractIntegrationTest{


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
                .password("dummy_password") // Will not be evaluated for JWT generation
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
    void createAirport_ShouldReturn201_WhenPayloadIsValidAndUserIsAdmin() {
        AirportRequest airportRequest = new AirportRequest("JFK", "John F. Kennedy", "New York", "USA");

        webTestClient.post()
            .uri("/api/v1/airports")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(airportRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id").isNumber()
            .jsonPath("$.code").isEqualTo("JFK")
            .jsonPath("$.city").isEqualTo("New York");

    }

    @Test
    void createAirport_ShouldReturn400_WhenPayloadIsInValid() {
         AirportRequest airportRequest = new AirportRequest("", "", "", "");

        webTestClient.post()
            .uri("/api/v1/airports")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(airportRequest)
            .exchange()
            .expectStatus().isBadRequest()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo(400)
            .jsonPath("$.message").isNotEmpty();
    }

    @Test
    void getAllAirports_ShouldReturn200__WhenQueryParametersAreValid(){

        webTestClient.get()
            .uri("/api/v1/airports?size=10&sort=name")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON);
            
    }

}
