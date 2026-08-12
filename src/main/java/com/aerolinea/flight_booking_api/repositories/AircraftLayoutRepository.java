package com.aerolinea.flight_booking_api.repositories;

import com.aerolinea.flight_booking_api.models.AircraftLayout;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AircraftLayoutRepository  extends JpaRepository<AircraftLayout, Long> {
}
