package com.aerolinea.flight_booking_api.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aerolinea.flight_booking_api.dtos.flight.FlightSearchResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.aerolinea.flight_booking_api.config.RestPageImpl;
import com.aerolinea.flight_booking_api.dtos.FlightSearchCriteria;
import com.aerolinea.flight_booking_api.models.FlightInstance;
import com.aerolinea.flight_booking_api.models.enums.SeatReservationStatus;
import com.aerolinea.flight_booking_api.repositories.FlightInstanceRepository;
import com.aerolinea.flight_booking_api.repositories.SeatRepository;
import com.aerolinea.flight_booking_api.repositories.SeatReservationRepository;
import com.aerolinea.flight_booking_api.specifications.FlightInstanceSpecification;

import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FlightServiceImpl implements FlightService {

    private static final List<SeatReservationStatus> OCCUPYING_STATUSES =
            List.of(SeatReservationStatus.HELD, SeatReservationStatus.CONFIRMED);

    private final FlightInstanceRepository flightInstanceRepository;
    private final SeatRepository seatRepository;
    private final SeatReservationRepository seatReservationRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "flightSearchCache", keyGenerator = "flightSearchKeyGenerator")
    public Page<FlightSearchResultDTO> searchFlights(FlightSearchCriteria criteria, Pageable pageable) {

        Page<FlightInstance> instancePage = flightInstanceRepository.findAll(
                FlightInstanceSpecification.hasDeparture(criteria.departure())
                        .and(FlightInstanceSpecification.hasDestination(criteria.destination()))
                        .and(FlightInstanceSpecification.hasPriceGreaterThanOrEqualTo(criteria.minPrice()))
                        .and(FlightInstanceSpecification.hasPriceLessThanOrEqualTo(criteria.maxPrice()))
                        .and(FlightInstanceSpecification.hasMinimumAvailableSeats(criteria.minAvailableSeats()))
                        .and(FlightInstanceSpecification.departsOnDate(criteria.date())),
                pageable);

        Map<Long, Long> availability = resolveAvailability(instancePage.getContent());

        List<FlightSearchResultDTO> content = instancePage.getContent().stream()
                .map(instance -> toSearchResult(instance, availability.getOrDefault(instance.getId(), 0L)))
                .toList();

        return new RestPageImpl<>(content, pageable.getPageNumber(), pageable.getPageSize(),
                instancePage.getTotalElements());
    }

    private Map<Long, Long> resolveAvailability(List<FlightInstance> instances) {
        if (instances.isEmpty()) {
            return Map.of();
        }

        List<Long> ids = instances.stream().map(FlightInstance::getId).toList();

        Map<Long, Long> capacity = new HashMap<>();
        seatRepository.countSeatsGroupedByFlightInstance(ids)
                .forEach(row -> capacity.put(row.getFlightInstanceId(), row.getTotal()));

        Map<Long, Long> occupied = new HashMap<>();
        seatReservationRepository.countOccupiedGroupedByFlightInstance(ids, OCCUPYING_STATUSES)
                .forEach(row -> occupied.put(row.getFlightInstanceId(), row.getTotal()));

        Map<Long, Long> available = new HashMap<>();
        ids.forEach(id -> available.put(id,
                capacity.getOrDefault(id, 0L) - occupied.getOrDefault(id, 0L)));

        return available;
    }

    private FlightSearchResultDTO toSearchResult(FlightInstance instance, long availableSeats) {
        return new FlightSearchResultDTO(
                instance.getId(),
                instance.getFlightSchedule().getFlightNumber(),
                instance.getFlightSchedule().getDepartureAirport().getCode(),
                instance.getDepartureAt(),
                instance.getFlightSchedule().getArrivalAirport().getCode(),
                instance.getArrivalAt(),
                availableSeats,
                instance.getFlightSchedule().getBasePrice());
    }
}