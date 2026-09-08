package com.aerolinea.flight_booking_api.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aerolinea.flight_booking_api.models.FlightSegment;

public interface FlightSegmentRepository extends JpaRepository<FlightSegment, Long> {

    List<FlightSegment> findByItineraryIdOrderBySegmentOrderAsc(Long itineraryId);

    long countByFlightInstanceId(Long flightInstanceId);
}