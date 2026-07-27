package com.aerolinea.flight_booking_api.dtos.aircraftmodel;


public record AircraftModelResponse(
    Long id,
    String manufacturer,
    String modelName,
    Short maxCapacity
){}
