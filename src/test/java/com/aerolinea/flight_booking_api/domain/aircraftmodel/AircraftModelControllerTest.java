package com.aerolinea.flight_booking_api.domain.aircraftmodel;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.aerolinea.flight_booking_api.controllers.AircraftModelController;
import com.aerolinea.flight_booking_api.dtos.aircraftmodel.AircraftModelRequest;
import com.aerolinea.flight_booking_api.dtos.aircraftmodel.AircraftModelResponse;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.aerolinea.flight_booking_api.exceptions.ResourceNotFoundException;
import com.aerolinea.flight_booking_api.security.CustomAccessDeniedHandler;
import com.aerolinea.flight_booking_api.security.JwtAuthenticationEntryPoint;
import com.aerolinea.flight_booking_api.security.SecurityConfig;
import com.aerolinea.flight_booking_api.services.AircraftModelService;
import com.aerolinea.flight_booking_api.services.JwtService;

@WebMvcTest(AircraftModelController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
public class AircraftModelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AircraftModelService aircraftModelService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private AircraftModelRequest aircraftModelRequest;
    private AircraftModelResponse aircraftModelResponse;
    private static String validJsonPayload;

    @BeforeAll
    static void setupAll() {
        validJsonPayload = """
                {
                    "manufacturer": "Airbus",
                    "modelName": "A320neo",
                    "maxCapacity": 195
                }
                """;
    }

    @BeforeEach
    void setUp() {
        aircraftModelRequest = new AircraftModelRequest("Airbus", "A320neo", (short) 195);
        aircraftModelResponse = new AircraftModelResponse(1L, "Airbus", "A320neo", (short) 195);
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAircraftModelById_shouldReturnResponse_whenExists() throws Exception {
        Long id = 1L;
        when(aircraftModelService.getAircraftModelById(id)).thenReturn(aircraftModelResponse);

        mockMvc.perform(get("/api/v1/aircraft-models/" + id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.manufacturer").value("Airbus"))
                .andExpect(jsonPath("$.modelName").value("A320neo"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAircraftModelById_shouldReturn404_whenNotFound() throws Exception {
        Long id = 9999L;
        when(aircraftModelService.getAircraftModelById(id)).thenThrow(
                new ResourceNotFoundException(ErrorCode.AIRCRAFT_MODEL_NOT_FOUND,
                        String.format(ErrorCode.AIRCRAFT_MODEL_NOT_FOUND.getMessage(), id)));

        mockMvc.perform(get("/api/v1/aircraft-models/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(String.format(ErrorCode.AIRCRAFT_MODEL_NOT_FOUND.getMessage(), id)));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllAircraftModels_shouldReturnPage_whenNotEmpty() throws Exception {
        Page<AircraftModelResponse> page = new PageImpl<>(List.of(aircraftModelResponse));

        when(aircraftModelService.getAllAircraftModels(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/aircraft-models")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createAircraftModel_shouldReturn403_WhenUserLacksAdminRole() throws Exception {
        mockMvc.perform(post("/api/v1/aircraft-models")
                        .content(validJsonPayload)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(ErrorCode.ACCESS_DENIED.getMessage()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAircraftModel_shouldReturnResponse_WhenSuccessful() throws Exception {
        when(aircraftModelService.createAircraftModel(aircraftModelRequest)).thenReturn(aircraftModelResponse);

        mockMvc.perform(post("/api/v1/aircraft-models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJsonPayload))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.manufacturer").value("Airbus"));
    }

    @Test
    void createAircraftModel_shouldReturn401_WhenUserIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/aircraft-models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJsonPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_OR_MISSING_TOKEN.getMessage()))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAircraftModel_shouldReturnResponse_WhenSuccessful() throws Exception {
        Long id = 1L;
        when(aircraftModelService.updateAircraftModel(id, aircraftModelRequest)).thenReturn(aircraftModelResponse);

        mockMvc.perform(put("/api/v1/aircraft-models/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJsonPayload))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.modelName").value("A320neo"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAircraftModel_shouldReturn404_whenNotFound() throws Exception {
        Long id = 9999L;
        when(aircraftModelService.updateAircraftModel(id, aircraftModelRequest)).thenThrow(
                new ResourceNotFoundException(ErrorCode.AIRCRAFT_MODEL_NOT_FOUND,
                        String.format(ErrorCode.AIRCRAFT_MODEL_NOT_FOUND.getMessage(), id)));

        mockMvc.perform(put("/api/v1/aircraft-models/" + id)
                        .content(validJsonPayload)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(String.format(ErrorCode.AIRCRAFT_MODEL_NOT_FOUND.getMessage(), id)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteAircraftModel_shouldReturn204_WhenSuccessful() throws Exception {
        Long id = 1L;
        doNothing().when(aircraftModelService).deleteAircraftModel(id);

        mockMvc.perform(delete("/api/v1/aircraft-models/" + id))
                .andExpect(status().isNoContent());

        verify(aircraftModelService).deleteAircraftModel(id);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteAircraftModel_shouldReturn404_whenNotFound() throws Exception {
        Long id = 9999L;
        doThrow(new ResourceNotFoundException(ErrorCode.AIRCRAFT_MODEL_NOT_FOUND,
                String.format(ErrorCode.AIRCRAFT_MODEL_NOT_FOUND.getMessage(), id)))
                .when(aircraftModelService).deleteAircraftModel(id);

        mockMvc.perform(delete("/api/v1/aircraft-models/" + id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(String.format(ErrorCode.AIRCRAFT_MODEL_NOT_FOUND.getMessage(), id)));

        verify(aircraftModelService).deleteAircraftModel(id);
    }
}