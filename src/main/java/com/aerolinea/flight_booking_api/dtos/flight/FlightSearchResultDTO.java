package com.aerolinea.flight_booking_api.dtos.flight;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FlightSearchResultDTO(
        Long flightInstanceId,
        String flightNumber,
        String departureAirport,
        LocalDateTime departureAt,
        String arrivalAirport,
        LocalDateTime arrivalAt,
        long availableSeats,
        BigDecimal basePrice) {
}