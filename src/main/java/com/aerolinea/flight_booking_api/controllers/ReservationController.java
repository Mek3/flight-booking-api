package com.aerolinea.flight_booking_api.controllers;

import org.springframework.web.bind.annotation.RestController;

import com.aerolinea.flight_booking_api.dtos.ReservationDTO;
import com.aerolinea.flight_booking_api.services.ReservationService;

import lombok.AllArgsConstructor;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;


@RestController
@AllArgsConstructor
@RequestMapping("/api/v1")
public class ReservationController {

    private final ReservationService reservationService;


    @PostMapping("/reservations")
    public ResponseEntity<ReservationDTO> createReservationDTO(@RequestBody ReservationDTO reservationDTO)  {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.createReservation(reservationDTO));
    }
    
}
