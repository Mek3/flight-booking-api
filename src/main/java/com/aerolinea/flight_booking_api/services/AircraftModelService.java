package com.aerolinea.flight_booking_api.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.aerolinea.flight_booking_api.dtos.aircraftmodel.AircraftModelRequest;
import com.aerolinea.flight_booking_api.dtos.aircraftmodel.AircraftModelResponse;

public interface AircraftModelService {
    AircraftModelResponse getAircraftModelById(Long id);
    Page<AircraftModelResponse> getAllAircraftModels(Pageable pageable);
    AircraftModelResponse createAircraftModel(AircraftModelRequest aircraftModelRequest);
    AircraftModelResponse updateAircraftModel(Long id, AircraftModelRequest aircraftModelRequest);
    void deleteAircraftModel(Long id);
}

