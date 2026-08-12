package com.aerolinea.flight_booking_api.services;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface SeatService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)

    int generateSeatsForInstance(Long flightInstanceId, Integer totalRows, String seatLetters);
}
