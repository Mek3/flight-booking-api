package com.aerolinea.flight_booking_api.domain.route;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.aerolinea.flight_booking_api.config.AbstractWebIntegrationTest;
import com.aerolinea.flight_booking_api.dtos.route.RouteRequest;
import com.aerolinea.flight_booking_api.dtos.route.RouteResponse;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.aerolinea.flight_booking_api.models.Airport;
import com.aerolinea.flight_booking_api.repositories.AirportRepository;

public class RouteIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired
    private AirportRepository airportRepository;

    private Long airportId1;
    private Long airportId2;
    private Long airportId3;

    @BeforeEach
    void setUp() {
        Airport airport1 = airportRepository.save(Airport.builder()
                .code("MAD-INT")
                .name("Madrid Barajas")
                .city("Madrid")
                .country("Spain")
                .build());

        Airport airport2 = airportRepository.save(Airport.builder()
                .code("JFK-INT")
                .name("JFK Airport")
                .city("New York")
                .country("USA")
                .build());
                
        Airport airport3 = airportRepository.save(Airport.builder()
                .code("LHR-INT")
                .name("Heathrow")
                .city("London")
                .country("UK")
                .build());

        airportId1 = airport1.getId();
        airportId2 = airport2.getId();
        airportId3 = airport3.getId();
    }

    @Test
    void createRoute_ShouldReturn201_WhenPayloadIsValidAndUserIsAdmin() {
        RouteRequest request = new RouteRequest(airportId1, airportId2);

        webTestClient.post()
            .uri("/api/v1/routes")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id").isNumber()
            .jsonPath("$.originAirport.id").isEqualTo(airportId1)
            .jsonPath("$.destinationAirport.id").isEqualTo(airportId2);
    }

    @Test
    void createRoute_ShouldReturn400_WhenPayloadIsInvalid() {
        RouteRequest request = new RouteRequest(null, -5L);

        webTestClient.post()
            .uri("/api/v1/routes")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo(HttpStatus.BAD_REQUEST.value())
            .jsonPath("$.internalCode").isEqualTo(ErrorCode.VALIDATION_ERROR.getCode())
            .jsonPath("$.path").isEqualTo("/api/v1/routes")
            .jsonPath("$.message").isNotEmpty();
    }

    @Test
    void createRoute_ShouldReturn409_WhenOriginAndDestinationAreSame() {
        RouteRequest request = new RouteRequest(airportId1, airportId1);

        webTestClient.post()
            .uri("/api/v1/routes")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT) 
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo(HttpStatus.CONFLICT.value())
            .jsonPath("$.internalCode").isEqualTo(ErrorCode.ROUTE_SAME_ORIGIN_DESTINATION.getCode())
            .jsonPath("$.path").isEqualTo("/api/v1/routes")
            .jsonPath("$.message").isNotEmpty();
    }

    @Test
    void getAllRoutes_ShouldReturn200_WhenQueryParametersAreValid() {
        webTestClient.get()
            .uri("/api/v1/routes?size=5&sort=id,desc")
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
    void deleteRoute_ShouldReturn204_WhenUserIsAdminAndIdExists() {
        Integer idToDelete = webTestClient.post()
            .uri("/api/v1/routes")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(new RouteRequest(airportId2, airportId3))
            .exchange()
            .expectStatus().isCreated()
            .expectBody(RouteResponse.class)
            .returnResult()
            .getResponseBody()
            .id().intValue();

        webTestClient.delete()
            .uri("/api/v1/routes/" + idToDelete)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
            .exchange()
            .expectStatus().isNoContent();
            
        webTestClient.get()
            .uri("/api/v1/routes/" + idToDelete)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("$.status").isEqualTo(HttpStatus.NOT_FOUND.value())
            .jsonPath("$.internalCode").isEqualTo(ErrorCode.ROUTE_NOT_FOUND.getCode())
            .jsonPath("$.path").isEqualTo("/api/v1/routes/" + idToDelete);
    }
}