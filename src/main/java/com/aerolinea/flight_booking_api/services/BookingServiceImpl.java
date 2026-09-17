package com.aerolinea.flight_booking_api.services;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.aerolinea.flight_booking_api.dtos.booking.BookingRequest;
import com.aerolinea.flight_booking_api.dtos.booking.BookingDTO;
import com.aerolinea.flight_booking_api.mappers.BookingMapper;
import com.aerolinea.flight_booking_api.models.*;
import com.aerolinea.flight_booking_api.repositories.*;
import com.aerolinea.flight_booking_api.services.routing.ItineraryRoutingService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.aerolinea.flight_booking_api.exceptions.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final UserRepository userRepository;
    private final FlightInstanceRepository flightInstanceRepository;
    private final ReservationRepository reservationRepository;
    private final ItineraryRoutingService itineraryRoutingService;
    private final BookingFactory bookingFactory;
    private final BookingMapper bookingMapper;
    private final SeatReservationService seatReservationService;

    @Transactional
    @CacheEvict(value = "flightSearchCache", allEntries = true)
    public BookingDTO createBooking(BookingRequest request) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND,
                        String.format(ErrorCode.USER_NOT_FOUND.getMessage(), username)));

        List<BookingRequest.FlightSegmentRequest> segments = request.itineraries().stream()
                .flatMap(itinerary -> itinerary.flightSegments().stream())
                .toList();

        seatReservationService.validateSeatSelection(segments, request.numberOfPassengers());

        Map<Long, FlightInstance> instances = loadInstances(bookingFactory.extractAndValidateInstanceIds(request));

        seatReservationService.validateSeatsBelongToFlights(segments);

        Reservation reservation = bookingFactory.assemble(request, user, instances);

        for (Itinerary itinerary : reservation.getItineraries()) {
            itineraryRoutingService.validate(itinerary);
        }

        List<Long> sortedSeatIds = segments.stream()
                .flatMap(segment -> segment.seatIds().stream())
                .sorted()
                .toList();
        List<Seat> lockedSeats = seatReservationService.acquireSeatLocksAndValidate(sortedSeatIds);

        Reservation saved = reservationRepository.save(reservation);

        seatReservationService.createHoldsForReservation(saved, lockedSeats);

        log.info("Booking created. Code: {} | User: {} | Itineraries: {} | Passengers: {}",
                saved.getReservationCode(), username, saved.getItineraries().size(), saved.getNumberOfPassengers());

        return bookingMapper.toBookingDTO(saved);
    }

    private Map<Long, FlightInstance> loadInstances(List<Long> ids) {
        List<FlightInstance> found = flightInstanceRepository.findByIdInWithSchedule(ids);

        if (found.size() != ids.size()) {
            Set<Long> foundIds = found.stream()
                    .map(FlightInstance::getId)
                    .collect(Collectors.toSet());

            Long missing = ids.stream()
                    .filter(id -> !foundIds.contains(id))
                    .findFirst()
                    .orElse(null);

            throw new ResourceNotFoundException(ErrorCode.FLIGHT_INSTANCE_NOT_FOUND,
                    String.format(ErrorCode.FLIGHT_INSTANCE_NOT_FOUND.getMessage(), missing));
        }

        return found.stream().collect(Collectors.toMap(FlightInstance::getId, Function.identity()));
    }
}