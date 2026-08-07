package com.aerolinea.flight_booking_api.repositories;

import com.aerolinea.flight_booking_api.models.FlightInstance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlightInstanceRepository  extends JpaRepository<FlightInstance, Long> {
}
