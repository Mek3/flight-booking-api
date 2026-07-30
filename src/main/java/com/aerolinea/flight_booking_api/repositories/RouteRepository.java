package com.aerolinea.flight_booking_api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aerolinea.flight_booking_api.models.Route;

public interface RouteRepository  extends JpaRepository<Route, Long>{

    boolean existsByOriginAirportIdAndDestinationAirportId(Long originAirportId, Long destinationAirportId);
}
