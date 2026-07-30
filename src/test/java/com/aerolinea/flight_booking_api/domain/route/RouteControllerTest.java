package com.aerolinea.flight_booking_api.domain.route;

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
import com.aerolinea.flight_booking_api.controllers.RouteController;
import com.aerolinea.flight_booking_api.dtos.airport.AirportResponse;
import com.aerolinea.flight_booking_api.dtos.route.RouteRequest;
import com.aerolinea.flight_booking_api.dtos.route.RouteResponse;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.aerolinea.flight_booking_api.exceptions.ResourceNotFoundException;
import com.aerolinea.flight_booking_api.services.RouteService;

@WebMvcTest(RouteController.class)
public class RouteControllerTest extends AbstractControllerTest {

    @MockitoBean
    private RouteService routeService;

    private RouteRequest routeRequest;
    private RouteResponse routeResponse;
    private static String validJsonPayload;

    @BeforeAll
    static void setupAll() {
        validJsonPayload = """
                {
                    "originAirportId": 10,
                    "destinationAirportId": 20
                }
                """;
    }

    @BeforeEach
    void setUp() {
        routeRequest = new RouteRequest(10L, 20L);
        
        AirportResponse originResponse = new AirportResponse(10L, "MAD", "Adolfo Suárez", "Madrid", "Spain");
        AirportResponse destinationResponse = new AirportResponse(20L, "JFK", "John F. Kennedy", "New York", "USA");
        
        routeResponse = new RouteResponse(1L, originResponse, destinationResponse);
    }

    @Test
    @WithMockUser(roles = "USER")
    void getRouteById_shouldReturnResponse_whenExists() throws Exception {
        Long id = 1L;
        when(routeService.getRouteById(id)).thenReturn(routeResponse);

        mockMvc.perform(get("/api/v1/routes/" + id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.originAirport.code").value("MAD"))
                .andExpect(jsonPath("$.destinationAirport.code").value("JFK"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getRouteById_shouldReturn404_whenNotFound() throws Exception {
        Long id = 9999L;
        when(routeService.getRouteById(id)).thenThrow(
                new ResourceNotFoundException(ErrorCode.ROUTE_NOT_FOUND,
                        String.format(ErrorCode.ROUTE_NOT_FOUND.getMessage(), id)));

        mockMvc.perform(get("/api/v1/routes/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(String.format(ErrorCode.ROUTE_NOT_FOUND.getMessage(), id)));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllRoutes_shouldReturnPage_whenNotEmpty() throws Exception {
        Page<RouteResponse> page = new PageImpl<>(List.of(routeResponse));

        when(routeService.getAllRoutes(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/routes")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createRoute_shouldReturn403_WhenUserLacksAdminRole() throws Exception {
        mockMvc.perform(post("/api/v1/routes")
                        .content(validJsonPayload)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(ErrorCode.ACCESS_DENIED.getMessage()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createRoute_shouldReturnResponse_WhenSuccessful() throws Exception {
        when(routeService.createRoute(routeRequest)).thenReturn(routeResponse);

        mockMvc.perform(post("/api/v1/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJsonPayload))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.originAirport.id").value(10L));
    }

    @Test
    void createRoute_shouldReturn401_WhenUserIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJsonPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_OR_MISSING_TOKEN.getMessage()))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateRoute_shouldReturnResponse_WhenSuccessful() throws Exception {
        Long id = 1L;
        when(routeService.updateRoute(id, routeRequest)).thenReturn(routeResponse);

        mockMvc.perform(put("/api/v1/routes/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJsonPayload))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.destinationAirport.id").value(20L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateRoute_shouldReturn404_whenNotFound() throws Exception {
        Long id = 9999L;
        when(routeService.updateRoute(id, routeRequest)).thenThrow(
                new ResourceNotFoundException(ErrorCode.ROUTE_NOT_FOUND,
                        String.format(ErrorCode.ROUTE_NOT_FOUND.getMessage(), id)));

        mockMvc.perform(put("/api/v1/routes/" + id)
                        .content(validJsonPayload)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(String.format(ErrorCode.ROUTE_NOT_FOUND.getMessage(), id)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteRoute_shouldReturn204_WhenSuccessful() throws Exception {
        Long id = 1L;
        doNothing().when(routeService).deleteRoute(id);

        mockMvc.perform(delete("/api/v1/routes/" + id))
                .andExpect(status().isNoContent());

        verify(routeService).deleteRoute(id);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteRoute_shouldReturn404_whenNotFound() throws Exception {
        Long id = 9999L;
        doThrow(new ResourceNotFoundException(ErrorCode.ROUTE_NOT_FOUND,
                String.format(ErrorCode.ROUTE_NOT_FOUND.getMessage(), id)))
                .when(routeService).deleteRoute(id);

        mockMvc.perform(delete("/api/v1/routes/" + id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(String.format(ErrorCode.ROUTE_NOT_FOUND.getMessage(), id)));

        verify(routeService).deleteRoute(id);
    }
}