package com.aerolinea.flight_booking_api.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.aerolinea.flight_booking_api.config.AbstractIntegrationTest;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private  MockMvc mockMvc;

    @Test
    @DisplayName("Should return 401 unauthorized when no JWT is provided")
    void shouldReturnUnauthorizedWhenNoJwtIsProvided() throws Exception {
        mockMvc.perform(post("/api/v1/reservations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_OR_MISSING_TOKEN.getMessage()))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.internalCode").value(ErrorCode.INVALID_OR_MISSING_TOKEN.getCode()))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Should return 403 forbiden when a USER role tries to access an ADMIN endpoint")
    @WithMockUser(roles = "USER")
    void shouldReturnForbiddenWhenUserRoleTriesToAccessAdminEndpoint() throws Exception {
      mockMvc.perform(delete("/api/v1/flights/1"))
                .andExpect(status().isForbidden())
                .andDo(print())
                .andExpect(jsonPath("$.message").value(ErrorCode.ACCESS_DENIED.getMessage()))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.internalCode").value(ErrorCode.ACCESS_DENIED.getCode()))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    
    @Test
    @DisplayName("Should allow access and return 404 Not Found when an ADMIN tries to delete a non-existent flight")
    @WithMockUser(roles = "ADMIN")
    void shouldReturnNotFoundWhenAdminRoleAccessesNonExistentAdminEndpoint() throws Exception {
        Long nonExistentFlightId = 9999L; 
      mockMvc.perform(delete("/api/v1/flights/"+ nonExistentFlightId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(String.format(ErrorCode.FLIGHT_NOT_FOUND.getMessage(), nonExistentFlightId)))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.internalCode").value(ErrorCode.FLIGHT_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("IDOR test: Should return 404 not found when a USER tries to access another user's reservation")
    @WithMockUser(roles = "USER")
    void shouldReturnNotFoundWhenUserRoleTriesToAccessAnotherUserReservation() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/me/2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(String.format(ErrorCode.RESERVATION_NOT_FOUND.getMessage(), 2)))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.internalCode").value(ErrorCode.RESERVATION_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }



}
