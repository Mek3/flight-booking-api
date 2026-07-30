package com.aerolinea.flight_booking_api.dtos.route;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RouteRequest(
    
    @NotNull(message = "Origin airport ID is mandatory")
    @Positive(message = "Origin airport ID must be a positive number")
    Long originAirportId,

    @NotNull(message = "Destination airport ID is mandatory")
    @Positive(message = "Destination airport ID must be a positive number")
    Long destinationAirportId
) {
}