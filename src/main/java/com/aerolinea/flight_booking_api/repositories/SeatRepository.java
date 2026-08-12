package com.aerolinea.flight_booking_api.repositories;

import com.aerolinea.flight_booking_api.models.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, Long>, SeatRepositoryCustom  {
}
