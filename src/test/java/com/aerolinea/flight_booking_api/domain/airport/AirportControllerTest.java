package com.aerolinea.flight_booking_api.domain.airport;

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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

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

import com.aerolinea.flight_booking_api.controllers.AirportController;
import com.aerolinea.flight_booking_api.dtos.airport.AirportRequest;
import com.aerolinea.flight_booking_api.dtos.airport.AirportResponse;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.aerolinea.flight_booking_api.exceptions.ResourceNotFoundException;
import com.aerolinea.flight_booking_api.models.Airport;
import com.aerolinea.flight_booking_api.security.CustomAccessDeniedHandler;
import com.aerolinea.flight_booking_api.security.JwtAuthenticationEntryPoint;
import com.aerolinea.flight_booking_api.security.SecurityConfig;
import com.aerolinea.flight_booking_api.services.AirportService;
import com.aerolinea.flight_booking_api.services.JwtService;

@WebMvcTest(AirportController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
public class AirportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AirportService airportService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private Airport airport;
    private AirportRequest airportRequest;
    private AirportResponse airportResponse;
    private static String validJsonPayload;

    @BeforeAll
    static void setupAll() {
        validJsonPayload = """
                {
                    "code": "JFK",
                    "name": "John F. Kennedy",
                    "city": "New York",
                    "country": "USA"
                }
                """;
    }

    @BeforeEach
    void setUp(){
        airport = Airport.builder()
                .code("JFL")
                .city("New York")
                .country("USA")
                .name("John F. Kennedy")
                .build();

        ReflectionTestUtils.setField(airport, "id", 1L);

        airportRequest = new AirportRequest("JFK", "John F. Kennedy", "New York", "USA");
        airportResponse = new AirportResponse(1L, "JFK", "John F. Kennedy", "New York", "USA");

    }

    @Test
    @WithMockUser(roles = "USER")
    void getAirportById_shouldReturnResponse_whenIsExists() throws Exception {
        Long id= 1L;
        when(airportService.getAirportById(id)).thenReturn(airportResponse);
        
        mockMvc.perform(get("/api/v1/airports/" + id)
                        .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.id").value(id))
                        .andExpect(jsonPath("$.name").value("John F. Kennedy"));
    }
    
    @Test
    @WithMockUser(roles = "USER")
    void getAirportById_shouldReturn404_whenNotFound() throws Exception{
        Long id = 9999L;
        when(airportService.getAirportById(id)).thenThrow(
            new ResourceNotFoundException(ErrorCode.AIRPORT_NOT_FOUND,
                 String.format(ErrorCode.AIRPORT_NOT_FOUND.getMessage(), id)));

        mockMvc.perform(get("/api/v1/airports/" + id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value(String.format(ErrorCode.AIRPORT_NOT_FOUND.getMessage(), id)));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAirportAll_shouldReturnPage_whenNotEmpty() throws Exception {
        Page<AirportResponse> airportResponsePage = new PageImpl<>(List.of(airportResponse));

        when(airportService.getAllAirports(any(Pageable.class))).thenReturn(airportResponsePage);

        mockMvc.perform(get("/api/v1/airports")
                        .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createAirport_shouldReturn403_WhenUserLacksAdminRole() throws Exception{
       
        mockMvc.perform(post("/api/v1/airports")
            .content(validJsonPayload)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(ErrorCode.ACCESS_DENIED.getMessage()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAirport_shouldReturnResponse_WhenSuccessfully() throws Exception{

        when(airportService.createAirport(airportRequest)).thenReturn(airportResponse);
        
        mockMvc.perform(post("/api/v1/airports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJsonPayload))
                        .andExpect(status().isCreated())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.id").value(1L))
                        .andExpect(jsonPath("$.name").value("John F. Kennedy"));
    }

    @Test
    void createAirport_shouldReturn401_WhenUserIsUnauthorized() throws Exception{

        mockMvc.perform(post("/api/v1/airports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJsonPayload))
                        .andExpect(status().isUnauthorized())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_OR_MISSING_TOKEN.getMessage()))
                        .andExpect(jsonPath("$.status").value(401))
                        .andExpect(jsonPath("$.internalCode").value(ErrorCode.INVALID_OR_MISSING_TOKEN.getCode()))
                        .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAirport_shouldReturnResponse_WhenSuccessfully() throws Exception{
        Long id = 1L;
        when(airportService.updateAirport(id, airportRequest)).thenReturn(airportResponse);
        
        mockMvc.perform(put("/api/v1/airports/"+ id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJsonPayload))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.id").value(1L))
                        .andExpect(jsonPath("$.name").value("John F. Kennedy"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAirport_shouldReturn404_whenNotFound() throws Exception{
        Long id = 9999L;
        when(airportService.updateAirport(id, airportRequest)).thenThrow(
            new ResourceNotFoundException(ErrorCode.AIRPORT_NOT_FOUND,
                 String.format(ErrorCode.AIRPORT_NOT_FOUND.getMessage(), id)));

        mockMvc.perform(put("/api/v1/airports/" + id)
            .content(validJsonPayload)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value(String.format(ErrorCode.AIRPORT_NOT_FOUND.getMessage(), id)));
    
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteAirport_shouldReturnResponse_WhenSuccessfully() throws Exception {
        Long id = 1L;
        doNothing().when(airportService).deleteAirport(id);

        mockMvc.perform(delete("/api/v1/airports/" + id))
                .andExpect(status().isNoContent());

        verify(airportService).deleteAirport(id);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteAirport_shouldReturn404_whenNotFound() throws Exception {
        Long id = 9999L;
        
        doThrow(new ResourceNotFoundException(ErrorCode.AIRPORT_NOT_FOUND,
                 String.format(ErrorCode.AIRPORT_NOT_FOUND.getMessage(), id))).when(airportService).deleteAirport(id);

        mockMvc.perform(delete("/api/v1/airports/" + id)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(String.format(ErrorCode.AIRPORT_NOT_FOUND.getMessage(), id)));

        verify(airportService).deleteAirport(id);
    }

}
