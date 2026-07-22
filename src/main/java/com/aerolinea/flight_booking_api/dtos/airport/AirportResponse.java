package com.aerolinea.flight_booking_api.dtos.airport;

public record AirportResponse(
    Long id,
    String code,
    String name,
    String city,
    String country
) {

}
