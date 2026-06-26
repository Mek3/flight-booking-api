package com.aerolinea.flight_booking_api.controllers;

import org.springframework.web.bind.annotation.RestController;

import com.aerolinea.flight_booking_api.dtos.ReservationDTO;
import com.aerolinea.flight_booking_api.dtos.ReservationRequest;
import com.aerolinea.flight_booking_api.services.ReservationService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;



@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;


    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping()
    public ResponseEntity<ReservationDTO> createReservationDTO(@RequestBody @Valid ReservationRequest reservationRequest)  {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.createReservation(reservationRequest));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelReservation(@PathVariable Long id){
        reservationService.cancelReservation(id);
        return ResponseEntity.noContent().build();
    }


    @PreAuthorize("hasRole('USER')")
    @GetMapping("/me/{id}")
    public ResponseEntity<ReservationDTO> getReservationByIdAndUsername(@PathVariable Long id) {
        return ResponseEntity.ok().body(reservationService.getReservationByIdAndUsername(id));
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/me")
    public ResponseEntity<Page<ReservationDTO>> getReservationsByUsername(@PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok().body(reservationService.getReservationsByUsername(pageable));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ReservationDTO> getReservationById(@PathVariable Long id) {
        return ResponseEntity.ok().body(reservationService.getReservationById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping()
    public ResponseEntity<Page<ReservationDTO>> getReservations(@PageableDefault(size = 10, sort= "createdAt") Pageable pageable) {
        return ResponseEntity.ok().body(reservationService.getReservations(pageable));
    }
    
    
    
    
}
