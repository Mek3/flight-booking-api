package com.aerolinea.flight_booking_api.domain.aircraftmodel;


import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.aerolinea.flight_booking_api.config.BaseWebIntegrationTest;
import com.aerolinea.flight_booking_api.dtos.aircraftmodel.AircraftModelRequest;
import com.aerolinea.flight_booking_api.dtos.aircraftmodel.AircraftModelResponse;


public class AircraftModelIntegrationTest extends BaseWebIntegrationTest {


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