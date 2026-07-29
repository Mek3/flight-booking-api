package com.aerolinea.flight_booking_api.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.aerolinea.flight_booking_api.security.CustomAccessDeniedHandler;
import com.aerolinea.flight_booking_api.security.JwtAuthenticationEntryPoint;
import com.aerolinea.flight_booking_api.security.SecurityConfig;
import com.aerolinea.flight_booking_api.services.JwtService;

@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
public abstract class AbstractControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected JwtService jwtService;

    @MockitoBean
    protected UserDetailsService userDetailsService;
}