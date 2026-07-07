package com.aerolinea.flight_booking_api.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
public class SecurityIntegrationTest {

    @Autowired
    private  MockMvc mockMvc;

    @Test
    @DisplayName("Should return 401 unauthorized when no JWT is provided")
    public void shouldReturnUnauthorizedWhenNoJwtIsProvided() throws Exception {
        mockMvc.perform(post("/api/v1/reservations"))
                .andExpect(status().isUnauthorized());
    }
    

}
