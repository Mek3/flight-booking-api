package com.aerolinea.flight_booking_api.domain.aircraft;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.aerolinea.flight_booking_api.config.BaseWebIntegrationTest;
import com.aerolinea.flight_booking_api.dtos.aircraft.AircraftRequest;
import com.aerolinea.flight_booking_api.dtos.aircraft.AircraftResponse;
import com.aerolinea.flight_booking_api.dtos.aircraftmodel.AircraftModelRequest;
import com.aerolinea.flight_booking_api.dtos.aircraftmodel.AircraftModelResponse;

public class AircraftIntegrationTest extends BaseWebIntegrationTest {

    
    private Long createDependencyModel() {
        AircraftModelRequest modelRequest = new AircraftModelRequest("Boeing", "737-MAX-Test", (short) 200);

        return webTestClient.post()
                .uri("/api/v1/aircraft-models")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(modelRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AircraftModelResponse.class)
                .returnResult()
                .getResponseBody()
                .id();
    }

    @Test
    void createAircraft_ShouldReturn201_WhenPayloadIsValidAndUserIsAdmin() {
        Long validModelId = createDependencyModel();
        AircraftRequest request = new AircraftRequest("EC-LOK", 1500, validModelId);

        webTestClient.post()
                .uri("/api/v1/aircraft")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isNumber()
                .jsonPath("$.registrationNumber").isEqualTo("EC-LOK")
                .jsonPath("$.totalFlightHours").isEqualTo(1500)
                .jsonPath("$.aircraftModelId").isEqualTo(validModelId.intValue());
    }

    @Test
    void createAircraft_ShouldReturn400_WhenPayloadIsInvalid() {
        AircraftRequest request = new AircraftRequest("invalid_reg!", -100, null);

        webTestClient.post()
                .uri("/api/v1/aircraft")
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
    void getAllAircraft_ShouldReturn200_WhenQueryParametersAreValid() {
        webTestClient.get()
                .uri("/api/v1/aircraft?size=5&sort=registrationNumber")
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
    void deleteAircraft_ShouldReturn204_WhenUserIsAdminAndIdExists() {
        Long validModelId = createDependencyModel();
        
        Long idToDelete = webTestClient.post()
                .uri("/api/v1/aircraft")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AircraftRequest("N-12345", 300, validModelId))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AircraftResponse.class)
                .returnResult()
                .getResponseBody()
                .id();

        webTestClient.delete()
                .uri("/api/v1/aircraft/" + idToDelete)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                .exchange()
                .expectStatus().isNoContent();
                
        webTestClient.get()
                .uri("/api/v1/aircraft/" + idToDelete)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                .exchange()
                .expectStatus().isNotFound();
    }
}