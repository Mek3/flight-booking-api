package com.aerolinea.flight_booking_api.dtos.aircraft;

public record AircraftResponse(
    Long id,
    String registrationNumber,
    Integer totalFlightHours,
    Long aircraftModelId
) {}
