package com.aerolinea.flight_booking_api.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private  MockMvc mockMvc;

    @Test
    @DisplayName("Should return 401 unauthorized when no JWT is provided")
    void shouldReturnUnauthorizedWhenNoJwtIsProvided() throws Exception {
        mockMvc.perform(post("/api/v1/reservations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 403 forbiden when a USER role tries to access an ADMIN endpoint")
    @WithMockUser(roles = "USER")
    void shouldReturnForbiddenWhenUserRoleTriesToAccessAdminEndpoint() throws Exception {
      mockMvc.perform(delete("/api/v1/flights/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow access and return 404 Not Found when an ADMIN tries to delete a non-existent flight")
    @WithMockUser(roles = "ADMIN")
    void shouldReturnNotFoundWhenAdminRoleAccessesNonExistentAdminEndpoint() throws Exception {
      mockMvc.perform(delete("/api/v1/flights/1"))
                .andExpect(status().isNotFound());
    }

}
