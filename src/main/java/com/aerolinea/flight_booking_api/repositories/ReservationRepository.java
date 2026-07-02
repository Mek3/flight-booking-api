package com.aerolinea.flight_booking_api.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aerolinea.flight_booking_api.models.Reservation;
import com.aerolinea.flight_booking_api.models.ReservationStatus;


public interface ReservationRepository extends JpaRepository<Reservation, Long>{

    Page<Reservation> findByUserUsername(Pageable pageable, String username);

    Optional<Reservation> findByIdAndUserUsername(Long id, String username);

    List<Reservation> findByStatusAndCreatedAtBefore(ReservationStatus pending, LocalDateTime threshold);

    @Query("SELECT r.id FROM Reservation r WHERE r.status = :status AND r.createdAt < :threshold")
    List<Long> findExpiredReservationIds(@Param("status") ReservationStatus status, 
                                         @Param("threshold") LocalDateTime threshold);

    @EntityGraph(attributePaths = {"flight"})
    @Query("SELECT r FROM Reservation r WHERE r.id = :id")
    Optional<Reservation> findByIdWithFlight(Long id);
}
