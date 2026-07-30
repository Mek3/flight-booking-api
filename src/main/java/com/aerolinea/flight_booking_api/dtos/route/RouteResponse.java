package com.aerolinea.flight_booking_api.dtos.route;

import com.aerolinea.flight_booking_api.dtos.airport.AirportResponse;

public record RouteResponse(
    Long id,
    AirportResponse originAirport,
    AirportResponse destinationAirport
) {
}