package com.aerolinea.flight_booking_api.domain.airport;


import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.aerolinea.flight_booking_api.config.BaseWebIntegrationTest;
import com.aerolinea.flight_booking_api.dtos.airport.AirportRequest;


public class AirportIntegrationTest extends BaseWebIntegrationTest {


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
