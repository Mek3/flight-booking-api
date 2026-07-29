package com.aerolinea.flight_booking_api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aerolinea.flight_booking_api.models.AircraftModel;

public interface AircraftModelRepository extends JpaRepository <AircraftModel, Long> {

    
    boolean existsAircraftModelByManufacturerAndModelName(String manufacturer, String modelName); 
}
