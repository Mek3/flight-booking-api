package com.aerolinea.flight_booking_api.services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aerolinea.flight_booking_api.dtos.booking.BookingRequest;
import com.aerolinea.flight_booking_api.exceptions.BusinessRuleViolationException;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.aerolinea.flight_booking_api.exceptions.ResourceNotFoundException;
import com.aerolinea.flight_booking_api.models.Reservation;
import com.aerolinea.flight_booking_api.models.Seat;
import com.aerolinea.flight_booking_api.models.SeatReservation;
import com.aerolinea.flight_booking_api.models.enums.SeatReservationStatus;
import com.aerolinea.flight_booking_api.repositories.SeatRepository;
import com.aerolinea.flight_booking_api.repositories.SeatReservationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatReservationServiceImpl implements SeatReservationService {

    private final SeatRepository seatRepository;
    private final SeatReservationRepository seatReservationRepository;

    @Value("${app.booking.cart-ttl}")
    private Duration cartTtl;

    @Override
    public void validateSeatSelection(List<BookingRequest.FlightSegmentRequest> segments, int numberOfPassengers) {
        segments.forEach(segment -> {
            if (segment.seatIds().size() != new HashSet<>(segment.seatIds()).size()) {
                throw new BusinessRuleViolationException(ErrorCode.DUPLICATE_SEAT_IN_SEGMENT,
                        String.format(ErrorCode.DUPLICATE_SEAT_IN_SEGMENT.getMessage(), segment.seatIds()));
            }

            if (segment.seatIds().size() != numberOfPassengers) {
                throw new BusinessRuleViolationException(ErrorCode.SEAT_COUNT_MISMATCH,
                        String.format(ErrorCode.SEAT_COUNT_MISMATCH.getMessage(),
                                segment.seatIds().size(), numberOfPassengers));
            }
        });
    }

    @Override
    public void validateSeatsBelongToFlights(List<BookingRequest.FlightSegmentRequest> segments) {
        segments.forEach(segment -> {
            long seatsOnFlight = seatRepository.countSeatsByFlightInstanceIdAndSeatIds(
                    segment.flightInstanceId(), segment.seatIds());

            if (seatsOnFlight != segment.seatIds().size()) {
                throw new ResourceNotFoundException(ErrorCode.SEAT_NOT_ON_SEGMENT_FLIGHT,
                        String.format(ErrorCode.SEAT_NOT_ON_SEGMENT_FLIGHT.getMessage(),
                                segment.seatIds(), segment.flightInstanceId()));
            }
        });
    }

    @Override
    public List<Seat> acquireSeatLocksAndValidate(List<Long> seatIds) {
        List<Seat> seats;

        try {
            seats = seatRepository.lockSeats(seatIds);
        } catch (PessimisticLockingFailureException e) {
            log.warn("Could not acquire locks on seats {}: {}", seatIds, e.getMessage());
            throw new BusinessRuleViolationException(ErrorCode.SEAT_CURRENTLY_LOCKED,
                    String.format(ErrorCode.SEAT_CURRENTLY_LOCKED.getMessage(), seatIds));
        }

        if (seats.size() != seatIds.size()) {
            throw new ResourceNotFoundException(ErrorCode.SEAT_NOT_FOUND,
                    String.format(ErrorCode.SEAT_NOT_FOUND.getMessage(), seatIds));
        }

        validateExistingSeatReservations(seatIds);

        return seats;
    }

    private void validateExistingSeatReservations(List<Long> seatIds) {
        List<SeatReservation> seatReservations = seatReservationRepository.findBySeatIdIn(seatIds);

        if (seatReservations.isEmpty()) {
            return;
        }

        boolean hasConfirmed = seatReservations.stream()
                .anyMatch(sr -> sr.getStatus() == SeatReservationStatus.CONFIRMED);

        if (hasConfirmed) {
            throw new BusinessRuleViolationException(ErrorCode.SEAT_ALREADY_BOOKED,
                    String.format(ErrorCode.SEAT_ALREADY_BOOKED.getMessage(), String.join(", ", seatIds.stream().map(String::valueOf).toList())));
        }

        boolean hasHeld = seatReservations.stream()
                .anyMatch(sr -> sr.getStatus() == SeatReservationStatus.HELD);

        if (hasHeld) {
            throw new BusinessRuleViolationException(ErrorCode.SEAT_CURRENTLY_LOCKED,
                    String.format(ErrorCode.SEAT_CURRENTLY_LOCKED.getMessage(), String.join(", ", seatIds.stream().map(String::valueOf).toList())));
        }
    }

    @Override
    @Transactional
    public void createHoldsForReservation(Reservation reservation, List<Seat> lockedSeats) {
        List<SeatReservation> holds = new ArrayList<>();
        LocalDateTime heldUntil = LocalDateTime.now().plus(cartTtl);

        reservation.getItineraries().forEach(itinerary ->
                itinerary.getSegments().forEach(segment -> lockedSeats.stream()
                        .filter(seat -> seat.getFlightInstance().getId().equals(segment.getFlightInstance().getId()))
                        .forEach(seat -> holds.add(SeatReservation.builder()
                                .seat(seat)
                                .flightSegment(segment)
                                .heldUntil(heldUntil)
                                .build()))));

        seatReservationRepository.saveAll(holds);

        log.debug("Created {} seat holds for reservation {}, expiring at {}",
                holds.size(), reservation.getReservationCode(), heldUntil);
    }
}