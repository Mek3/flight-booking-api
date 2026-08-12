package com.aerolinea.flight_booking_api.dtos.Seat;

public record SeatGenerationProjection(Long flightInstanceId, Integer totalRows, String seatLetters) {}