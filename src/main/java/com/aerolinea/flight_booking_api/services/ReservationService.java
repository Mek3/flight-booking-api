package com.aerolinea.flight_booking_api.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.aerolinea.flight_booking_api.dtos.ReservationDTO;
import com.aerolinea.flight_booking_api.dtos.ReservationRequest;

public interface ReservationService {

     void cancelReservation(Long idReservation);

     Page<ReservationDTO> getReservationsByUsername(Pageable pageable);

     Page<ReservationDTO> getReservations(Pageable pageable);

     ReservationDTO getReservationByIdAndUsername(Long idReservation);

     ReservationDTO getReservationById(Long idReservation);

     void confirmReservation(Long id);

     void expirePendingReservations();

     void processSingleExpiration(Long idReservation);

}
