package com.aerolinea.flight_booking_api.services;

import com.aerolinea.flight_booking_api.models.FlightSchedule;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

public interface FlightInstanceService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void generateFlightInstance(FlightSchedule schedule, LocalDate departureDate);
}
