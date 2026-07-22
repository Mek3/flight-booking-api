package com.aerolinea.flight_booking_api.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.aerolinea.flight_booking_api.dtos.airport.AirportRequest;
import com.aerolinea.flight_booking_api.dtos.airport.AirportResponse;

public interface AirportService {
    AirportResponse getAirportById(Long id);
    Page<AirportResponse> getAllAirports(Pageable pageable);
    AirportResponse createAirport(AirportRequest airportRequest);
    AirportResponse updateAirport(Long id, AirportRequest airportRequest);
    void deleteAirport(Long id);
}
