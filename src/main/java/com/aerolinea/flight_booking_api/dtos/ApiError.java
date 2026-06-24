package com.aerolinea.flight_booking_api.dtos;

import java.time.LocalDateTime;

public record ApiError(
    LocalDateTime timestamp,
    int status,
    Long internalCode,
    String error,
    String message,
    String path
){}
