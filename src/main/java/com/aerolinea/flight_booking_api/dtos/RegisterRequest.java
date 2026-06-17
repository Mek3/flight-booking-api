package com.aerolinea.flight_booking_api.dtos;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
     @NotBlank String name,
     @NotBlank String surname,
     @NotBlank String password,
     @NotBlank String email,
     @NotBlank String username,
     @NotBlank String phone
 ) {}
