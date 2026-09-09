package com.aerolinea.flight_booking_api.dtos.seat;

public record SeatGenerationProjection(Long flightInstanceId, Integer totalRows, String seatLetters) {}