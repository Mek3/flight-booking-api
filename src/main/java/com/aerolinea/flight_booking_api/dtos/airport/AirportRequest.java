package com.aerolinea.flight_booking_api.dtos.airport;

import jakarta.validation.constraints.NotBlank;

public record AirportRequest(
    @NotBlank String code,
    @NotBlank String name,
    @NotBlank String city,
    @NotBlank String country
) {

}
