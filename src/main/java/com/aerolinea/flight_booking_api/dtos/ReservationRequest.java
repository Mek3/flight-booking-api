package com.aerolinea.flight_booking_api.dtos;

import jakarta.validation.constraints.NotNull;

public record ReservationRequest(
      @NotNull Long flightId,
      @NotNull Integer numberOfPassengers
) {}
