package com.aerolinea.flight_booking_api.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.aerolinea.flight_booking_api.dtos.booking.BookingRequest;
import org.springframework.stereotype.Component;

import com.aerolinea.flight_booking_api.models.FlightInstance;
import com.aerolinea.flight_booking_api.models.FlightSegment;
import com.aerolinea.flight_booking_api.models.Itinerary;
import com.aerolinea.flight_booking_api.models.Reservation;
import com.aerolinea.flight_booking_api.models.ReservationStatus;
import com.aerolinea.flight_booking_api.models.User;

@Component
public class BookingFactory {

    public Reservation assemble(BookingRequest request, User user, Map<Long, FlightInstance> instances) {

        Reservation reservation = Reservation.builder()
                .reservationCode(generateReservationCode())
                .status(ReservationStatus.PENDING)
                .numberOfPassengers(request.numberOfPassengers())
                .totalPrice(BigDecimal.ZERO)
                .user(user)
                .build();

        for (BookingRequest.ItineraryRequest itineraryRequest : request.itineraries()) {
            Itinerary itinerary = Itinerary.builder().build();

            for (Long flightInstanceId : itineraryRequest.flightInstanceIds()) {
                itinerary.addSegment(FlightSegment.builder()
                        .flightInstance(instances.get(flightInstanceId))
                        .build());
            }

            reservation.addItinerary(itinerary);
        }

        reservation.setTotalPrice(calculateTotalPrice(reservation, request.numberOfPassengers()));

        return reservation;
    }

    private BigDecimal calculateTotalPrice(Reservation reservation, int numberOfPassengers) {
        BigDecimal perPassenger = reservation.getItineraries().stream()
                .flatMap(itinerary -> itinerary.getSegments().stream())
                .map(this::segmentFare)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return perPassenger.multiply(BigDecimal.valueOf(numberOfPassengers));
    }

    private BigDecimal segmentFare(FlightSegment segment) {
        return segment.getFlightInstance().getFlightSchedule().getBasePrice();
    }

    private String generateReservationCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public List<Long> requestedInstanceIds(BookingRequest request) {
        return request.itineraries().stream()
                .flatMap(itinerary -> itinerary.flightInstanceIds().stream())
                .distinct()
                .toList();
    }
}
