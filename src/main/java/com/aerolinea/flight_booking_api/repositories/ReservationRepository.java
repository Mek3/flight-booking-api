package com.aerolinea.flight_booking_api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aerolinea.flight_booking_api.models.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, Long>{
    

}
