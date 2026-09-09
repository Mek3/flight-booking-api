package com.aerolinea.flight_booking_api.dtos.itinerary;

import com.aerolinea.flight_booking_api.dtos.flight.FlightSegmentDTO;

import java.util.List;

public record ItineraryDTO(
        Long id,
        Integer sequenceOrder,
        List<FlightSegmentDTO> segments) {
}
