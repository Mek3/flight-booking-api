package com.aerolinea.flight_booking_api.mappers;

import org.springframework.stereotype.Component;

import com.aerolinea.flight_booking_api.dtos.ReservationDTO;
import com.aerolinea.flight_booking_api.models.Reservation;

@Component
public class ReservationMapper {

    public ReservationDTO toDto(Reservation reservation) {

        if (reservation == null) {
            return null;
        }
        
        ReservationDTO reservationDTO = new ReservationDTO();
        reservationDTO.setId(reservation.getId());
        reservationDTO.setFlightId((reservation.getFlight() == null )? null : reservation.getFlight().getId());
        reservationDTO.setUserId((reservation.getUser() == null)? null : reservation.getUser().getId());
        reservationDTO.setNumberOfPassengers(reservation.getNumberOfPassengers());
        reservationDTO.setReservationCode(reservation.getReservationCode());
        reservationDTO.setStatus(reservation.getStatus());
        reservationDTO.setTotalPrice(reservation.getTotalPrice());
        reservationDTO.setCreatedAt(reservation.getCreatedAt());

        return reservationDTO;
    }
}
