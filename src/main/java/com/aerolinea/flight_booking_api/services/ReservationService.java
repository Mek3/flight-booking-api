package com.aerolinea.flight_booking_api.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.aerolinea.flight_booking_api.dtos.booking.BookingDTO;

public interface ReservationService {

     void cancelReservation(Long idReservation);

     Page<BookingDTO> getReservationsByUsername(Pageable pageable);

     Page<BookingDTO> getReservations(Pageable pageable);

     BookingDTO getReservationByIdAndUsername(Long idReservation);

     BookingDTO getReservationById(Long idReservation);

     void confirmReservation(Long id);

     void expirePendingReservations();

     void processSingleExpiration(Long idReservation);

}