package com.aerolinea.flight_booking_api.repositories;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.aerolinea.flight_booking_api.models.Reservation;


public interface ReservationRepository extends JpaRepository<Reservation, Long>{

    Page<Reservation> findByUserUsername(Pageable pageable, String username);

    Optional<Reservation> findByIdAndUserUsername(Long id, String username);
}
