package com.aerolinea.flight_booking_api.security;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.aerolinea.flight_booking_api.dtos.ApiError;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler{

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        
        String originalUri = (String) request.getAttribute("jakarta.servlet.forward.request_uri");
        String path = originalUri != null ? originalUri : request.getRequestURI();

        ApiError apiError = new ApiError(
            LocalDateTime.now(),
            HttpStatus.FORBIDDEN.value(),
            ErrorCode.INSUFFICIENT_PERMISSIONS.getCode(),
            HttpStatus.FORBIDDEN.getReasonPhrase(),
            "Forbidden: You do not have the required permissions to access this resource.",
            path
        );

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.registerModule(new JavaTimeModule());
        mapper.writeValue(response.getOutputStream(), apiError);
    }

}
