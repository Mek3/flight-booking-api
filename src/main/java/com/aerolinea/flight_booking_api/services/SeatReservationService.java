package com.aerolinea.flight_booking_api.services;

import java.util.List;

import com.aerolinea.flight_booking_api.dtos.booking.BookingRequest;
import com.aerolinea.flight_booking_api.models.Reservation;
import com.aerolinea.flight_booking_api.models.Seat;

public interface SeatReservationService {

    void validateSeatSelection(List<BookingRequest.FlightSegmentRequest> segments, int numberOfPassengers);

    void validateSeatsBelongToFlights(List<BookingRequest.FlightSegmentRequest> segments);

    List<Seat> acquireSeatLocksAndValidate(List<Long> seatIds);

    void createHoldsForReservation(Reservation reservation, List<Seat> lockedSeats);

    void cancelSeatReservationsForReservation(Long idReservation);

    void confirmSeatReservationsForReservation(Long idReservation);
}