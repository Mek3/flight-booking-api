package com.aerolinea.flight_booking_api.dtos.booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.aerolinea.flight_booking_api.dtos.itinerary.ItineraryDTO;
import com.aerolinea.flight_booking_api.models.ReservationStatus;

public record BookingDTO(
        Long id,
        Long userId,
        String reservationCode,
        ReservationStatus status,
        Integer numberOfPassengers,
        BigDecimal totalPrice,
        List<ItineraryDTO> itineraries,
        LocalDateTime createdAt) {
}
