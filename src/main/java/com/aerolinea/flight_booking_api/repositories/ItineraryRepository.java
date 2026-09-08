package com.aerolinea.flight_booking_api.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.aerolinea.flight_booking_api.models.Itinerary;

public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {

    @EntityGraph(attributePaths = {"segments", "segments.flightInstance", "segments.flightInstance.flightSchedule"})
    @Query("SELECT i FROM Itinerary i WHERE i.id = :id")
    Optional<Itinerary> findByIdWithSegments(Long id);

    @EntityGraph(attributePaths = {"segments", "segments.flightInstance", "segments.flightInstance.flightSchedule"})
    @Query("SELECT i FROM Itinerary i WHERE i.reservation.id = :reservationId ORDER BY i.sequenceOrder ASC")
    List<Itinerary> findByReservationIdWithSegments(Long reservationId);
}