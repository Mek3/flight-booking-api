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


public interface ReservationRepository extends JpaRepository<Reservation, Long>{

    Page<Reservation> findByUserUsername(Pageable pageable, String username);

    Optional<Reservation> findByIdAndUserUsername(Long id, String username);

    @Query("""
            SELECT DISTINCT sr.flightSegment.itinerary.reservation.id
            FROM SeatReservation sr
            WHERE sr.status = 'HELD'
              AND sr.heldUntil < :threshold
        """)
    List<Long> findExpiredReservationIds(@Param("threshold") LocalDateTime threshold);

    @EntityGraph(attributePaths = {"flightInstance", "flightInstance.flightSchedule"})
    @Query("SELECT r FROM Reservation r WHERE r.id = :id")
    Optional<Reservation> findByIdWithFlightInstance(Long id);

    @EntityGraph(attributePaths = {
            "itineraries",
            "itineraries.segments",
            "itineraries.segments.flightInstance",
            "itineraries.segments.flightInstance.flightSchedule"})
    @Query("SELECT r FROM Reservation r WHERE r.id = :id")
    Optional<Reservation> findByIdWithItineraries(Long id);

}