package com.aerolinea.flight_booking_api.models;

import java.time.LocalDateTime;

public record ApiError(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    String path
){}
