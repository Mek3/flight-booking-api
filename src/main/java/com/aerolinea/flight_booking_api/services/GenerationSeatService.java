package com.aerolinea.flight_booking_api.services;

import org.springframework.transaction.annotation.Transactional;

public interface GenerationSeatService {
    @Transactional
    void generateUpcomingSeat();
}
