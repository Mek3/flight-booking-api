package com.aerolinea.flight_booking_api.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aerolinea.flight_booking_api.dtos.FlightDTO;
import com.aerolinea.flight_booking_api.dtos.FlightSearchCriteria;
import com.aerolinea.flight_booking_api.services.FlightService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@RequestMapping("/api/v1/flights")
@AllArgsConstructor
public class FlightController {

    private final FlightService flightService;

    @GetMapping()
    public ResponseEntity<Page<FlightDTO>> getFlights(@PageableDefault(size = 10, sort = "departureTime") Pageable pageable) {
        return ResponseEntity.ok().body(flightService.getFlights(pageable));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping()
    public ResponseEntity<FlightDTO> postFlight(@RequestBody @Valid FlightDTO flightDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flightService.save(flightDTO));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<FlightDTO> putFlight(@PathVariable Long id, @RequestBody @Valid FlightDTO flightDTO) {
        return ResponseEntity.ok().body(flightService.updateFlight(id, flightDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightDTO> getFlight(@PathVariable Long id) {
        return ResponseEntity.ok().body(flightService.flightById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFlight(@PathVariable Long id) {
        flightService.deleteFlightById(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<Page<FlightDTO>> searchFlights(
            @ModelAttribute FlightSearchCriteria criteria,
            @PageableDefault(size = 10, sort = "departureTime") Pageable pageable) {
        return ResponseEntity.ok().body(flightService.searchFlights(criteria, pageable));
    }
    
    

}
