package com.aerolinea.flight_booking_api.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.aerolinea.flight_booking_api.dtos.aircraft.AircraftRequest;
import com.aerolinea.flight_booking_api.dtos.aircraft.AircraftResponse;


public interface AircraftService {
    AircraftResponse getAircraftById(Long id);
    Page<AircraftResponse> getAllAircraft(Pageable pageable);
    AircraftResponse createAircraft(AircraftRequest aircraftRequest);
    AircraftResponse updateAircraft(Long id, AircraftRequest aircraftRequest);
    void deleteAircraft(Long id);
}