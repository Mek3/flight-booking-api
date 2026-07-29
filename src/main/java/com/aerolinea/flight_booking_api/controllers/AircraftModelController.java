package com.aerolinea.flight_booking_api.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aerolinea.flight_booking_api.dtos.aircraftmodel.AircraftModelRequest;
import com.aerolinea.flight_booking_api.dtos.aircraftmodel.AircraftModelResponse;
import com.aerolinea.flight_booking_api.services.AircraftModelService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/aircraft-models")
public class AircraftModelController {

    private final AircraftModelService aircraftModelService;


     @GetMapping("/{id}")
    public ResponseEntity<AircraftModelResponse> getAircraftModelById(@PathVariable Long id) {
        return ResponseEntity.ok(aircraftModelService.getAircraftModelById(id));
    }

    @GetMapping
    public ResponseEntity<Page<AircraftModelResponse>> getAllAircraftModels(@PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(aircraftModelService.getAllAircraftModels(pageable));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<AircraftModelResponse> createAirport(@RequestBody @Valid AircraftModelRequest airportRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(aircraftModelService.createAircraftModel(airportRequest));
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<AircraftModelResponse> updateAirport(@PathVariable Long id, @RequestBody @Valid AircraftModelRequest aircraftModelRequest) {
        return ResponseEntity.ok(aircraftModelService.updateAircraftModel(id, aircraftModelRequest));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAirport(@PathVariable Long id) {
        aircraftModelService.deleteAircraftModel(id);

        return ResponseEntity.noContent().build();
    }
    

}
