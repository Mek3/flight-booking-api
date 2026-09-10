package com.aerolinea.flight_booking_api.security;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.aerolinea.flight_booking_api.dtos.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SecurityErrorResponder {

    private static final String FORWARDED_URI_ATTRIBUTE = "jakarta.servlet.forward.request_uri";

    private final ObjectMapper objectMapper;

    public void write(HttpServletRequest request, HttpServletResponse response,
                      HttpStatus status, Long errorCode, String message) throws IOException {

        ApiError apiError = new ApiError(
                LocalDateTime.now(),
                status.value(),
                errorCode,
                status.getReasonPhrase(),
                message,
                resolvePath(request)
        );

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(status.value());

        objectMapper.writeValue(response.getOutputStream(), apiError);
    }

    private String resolvePath(HttpServletRequest request) {
        String forwardedUri = (String) request.getAttribute(FORWARDED_URI_ATTRIBUTE);
        return forwardedUri != null ? forwardedUri : request.getRequestURI();
    }
}