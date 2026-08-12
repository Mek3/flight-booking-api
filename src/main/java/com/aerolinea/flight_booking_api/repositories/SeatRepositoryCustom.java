package com.aerolinea.flight_booking_api.repositories;

public interface SeatRepositoryCustom {
    int[] batchInsertSeats(Long flightInstanceId, Integer totalRows, String seatLetters);
}
