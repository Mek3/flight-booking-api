package com.aerolinea.flight_booking_api.services;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.aerolinea.flight_booking_api.dtos.booking.BookingRequest;
import com.aerolinea.flight_booking_api.exceptions.BusinessRuleViolationException;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
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

            for (BookingRequest.FlightSegmentRequest segmentRequest : itineraryRequest.flightSegments()) {
                itinerary.addSegment(FlightSegment.builder()
                        .flightInstance(instances.get(segmentRequest.flightInstanceId()))
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

    public List<Long> extractAndValidateInstanceIds(BookingRequest request) {
        List<Long> flightInstanceIds = request.itineraries().stream()
                .flatMap(itinerary -> itinerary.flightSegments().stream()
                        .map(BookingRequest.FlightSegmentRequest::flightInstanceId))
                .toList();

        if (flightInstanceIds.size() != new HashSet<>(flightInstanceIds).size()) {
            throw new BusinessRuleViolationException(ErrorCode.DUPLICATE_FLIGHT_INSTANCE_IN_BOOKING,
                    String.format(ErrorCode.DUPLICATE_FLIGHT_INSTANCE_IN_BOOKING.getMessage(), flightInstanceIds));
        }

        return flightInstanceIds;
    }
}
