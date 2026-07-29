package com.aerolinea.flight_booking_api.domain.aircraft;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.aerolinea.flight_booking_api.config.AbstractControllerTest;
import com.aerolinea.flight_booking_api.controllers.AircraftController;
import com.aerolinea.flight_booking_api.dtos.aircraft.AircraftRequest;
import com.aerolinea.flight_booking_api.dtos.aircraft.AircraftResponse;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.aerolinea.flight_booking_api.exceptions.ResourceNotFoundException;
import com.aerolinea.flight_booking_api.services.AircraftService;

@WebMvcTest(AircraftController.class)
public class AircraftControllerTest extends AbstractControllerTest{

    @MockitoBean
    private AircraftService aircraftService;

    private AircraftRequest aircraftRequest;
    private AircraftResponse aircraftResponse;
    private static String validJsonPayload;

    @BeforeAll
    static void setupAll() {
        validJsonPayload = """
                {
                    "registrationNumber": "EC-LOK",
                    "totalFlightHours": 5000,
                    "aircraftModelId": 10
                }
                """;
    }

    @BeforeEach
    void setUp() {
        aircraftRequest = new AircraftRequest("EC-LOK", 5000, 10L);
        aircraftResponse = new AircraftResponse(1L, "EC-LOK", 5000, 10L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAircraftById_shouldReturnResponse_whenExists() throws Exception {
        Long id = 1L;
        when(aircraftService.getAircraftById(id)).thenReturn(aircraftResponse);

        mockMvc.perform(get("/api/v1/aircraft/" + id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.registrationNumber").value("EC-LOK"))
                .andExpect(jsonPath("$.totalFlightHours").value(5000))
                .andExpect(jsonPath("$.aircraftModelId").value(10));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAircraftById_shouldReturn404_whenNotFound() throws Exception {
        Long id = 9999L;
        when(aircraftService.getAircraftById(id)).thenThrow(
                new ResourceNotFoundException(ErrorCode.AIRCRAFT_NOT_FOUND,
                        String.format(ErrorCode.AIRCRAFT_NOT_FOUND.getMessage(), id)));

        mockMvc.perform(get("/api/v1/aircraft/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(String.format(ErrorCode.AIRCRAFT_NOT_FOUND.getMessage(), id)));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllAircraft_shouldReturnPage_whenNotEmpty() throws Exception {
        Page<AircraftResponse> page = new PageImpl<>(List.of(aircraftResponse));

        when(aircraftService.getAllAircraft(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/aircraft")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createAircraft_shouldReturn403_WhenUserLacksAdminRole() throws Exception {
        mockMvc.perform(post("/api/v1/aircraft")
                        .content(validJsonPayload)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(ErrorCode.ACCESS_DENIED.getMessage()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAircraft_shouldReturnResponse_WhenSuccessful() throws Exception {
        when(aircraftService.createAircraft(aircraftRequest)).thenReturn(aircraftResponse);

        mockMvc.perform(post("/api/v1/aircraft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJsonPayload))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.registrationNumber").value("EC-LOK"));
    }

    @Test
    void createAircraft_shouldReturn401_WhenUserIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/aircraft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJsonPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_OR_MISSING_TOKEN.getMessage()))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAircraft_shouldReturnResponse_WhenSuccessful() throws Exception {
        Long id = 1L;
        when(aircraftService.updateAircraft(id, aircraftRequest)).thenReturn(aircraftResponse);

        mockMvc.perform(put("/api/v1/aircraft/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJsonPayload))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.registrationNumber").value("EC-LOK"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAircraft_shouldReturn404_whenNotFound() throws Exception {
        Long id = 9999L;
        when(aircraftService.updateAircraft(id, aircraftRequest)).thenThrow(
                new ResourceNotFoundException(ErrorCode.AIRCRAFT_NOT_FOUND,
                        String.format(ErrorCode.AIRCRAFT_NOT_FOUND.getMessage(), id)));

        mockMvc.perform(put("/api/v1/aircraft/" + id)
                        .content(validJsonPayload)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(String.format(ErrorCode.AIRCRAFT_NOT_FOUND.getMessage(), id)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteAircraft_shouldReturn204_WhenSuccessful() throws Exception {
        Long id = 1L;
        doNothing().when(aircraftService).deleteAircraft(id);

        mockMvc.perform(delete("/api/v1/aircraft/" + id))
                .andExpect(status().isNoContent());

        verify(aircraftService).deleteAircraft(id);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteAircraft_shouldReturn404_whenNotFound() throws Exception {
        Long id = 9999L;
        doThrow(new ResourceNotFoundException(ErrorCode.AIRCRAFT_NOT_FOUND,
                String.format(ErrorCode.AIRCRAFT_NOT_FOUND.getMessage(), id)))
                .when(aircraftService).deleteAircraft(id);

        mockMvc.perform(delete("/api/v1/aircraft/" + id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(String.format(ErrorCode.AIRCRAFT_NOT_FOUND.getMessage(), id)));

        verify(aircraftService).deleteAircraft(id);
    }
}