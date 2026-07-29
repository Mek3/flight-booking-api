package com.aerolinea.flight_booking_api.dtos.aircraft;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AircraftRequest(
    @NotBlank(message = "Registration number is mandatory")
    @Pattern(regexp = "^[A-Z0-9-]{3,10}$", message = "Invalid registration number format")
    String registrationNumber,

    @NotNull(message = "Total flight hours must be provided")
    @Min(value = 0, message = "Flight hours cannot be negative")
    Integer totalFlightHours,

    @NotNull(message = "Aircraft model ID cannot be null")
    Long aircraftModelId
) {}