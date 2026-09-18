package com.aerolinea.flight_booking_api.controllers;

import com.aerolinea.flight_booking_api.dtos.flight.FlightSearchResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aerolinea.flight_booking_api.dtos.FlightSearchCriteria;
import com.aerolinea.flight_booking_api.services.FlightService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;


@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    @GetMapping("/search")
    public ResponseEntity<Page<FlightSearchResultDTO>> searchFlights(
            @ModelAttribute FlightSearchCriteria criteria,
            @PageableDefault(size = 10, sort = "departureDate") Pageable pageable) {
        return ResponseEntity.ok().body(flightService.searchFlights(criteria, pageable));
    }
}
