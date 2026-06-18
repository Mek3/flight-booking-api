package com.aerolinea.flight_booking_api.services;

import com.aerolinea.flight_booking_api.dtos.ReservationDTO;
import com.aerolinea.flight_booking_api.dtos.ReservationRequest;

public interface ReservationService {

     public ReservationDTO createReservation(ReservationRequest reservationRequest);

}
