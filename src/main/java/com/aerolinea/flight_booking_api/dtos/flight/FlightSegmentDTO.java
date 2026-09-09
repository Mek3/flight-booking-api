package com.aerolinea.flight_booking_api.dtos.flight;

import java.time.LocalDateTime;

public record FlightSegmentDTO(
        Long id,
        Integer segmentOrder,
        Long flightInstanceId,
        String flightNumber,
        String departureAirport,
        LocalDateTime departureAt,
        String arrivalAirport,
        LocalDateTime arrivalAt) {
}