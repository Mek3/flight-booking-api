package com.aerolinea.flight_booking_api.specifications;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.aerolinea.flight_booking_api.models.FlightInstance;
import com.aerolinea.flight_booking_api.models.FlightSchedule;
import com.aerolinea.flight_booking_api.models.Seat;
import com.aerolinea.flight_booking_api.models.SeatReservation;
import com.aerolinea.flight_booking_api.models.enums.SeatReservationStatus;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public class FlightInstanceSpecification {

    private static final List<SeatReservationStatus> OCCUPYING_STATUSES =
            List.of(SeatReservationStatus.HELD, SeatReservationStatus.CONFIRMED);

    private FlightInstanceSpecification() {}

    public static Specification<FlightInstance> hasDeparture(String departureCode) {
        return (root, query, cb) -> {
            if (departureCode == null || departureCode.isBlank()) {
                return cb.conjunction();
            }
            Join<FlightInstance, FlightSchedule> schedule = root.join("flightSchedule", JoinType.INNER);
            return cb.equal(schedule.join("departureAirport", JoinType.INNER).get("code"), departureCode);
        };
    }

    public static Specification<FlightInstance> hasDestination(String destinationCode) {
        return (root, query, cb) -> {
            if (destinationCode == null || destinationCode.isBlank()) {
                return cb.conjunction();
            }
            Join<FlightInstance, FlightSchedule> schedule = root.join("flightSchedule", JoinType.INNER);
            return cb.equal(schedule.join("arrivalAirport", JoinType.INNER).get("code"), destinationCode);
        };
    }

    public static Specification<FlightInstance> hasPriceGreaterThanOrEqualTo(BigDecimal minPrice) {
        return (root, query, cb) -> {
            if (minPrice == null) {
                return cb.conjunction();
            }
            Join<FlightInstance, FlightSchedule> schedule = root.join("flightSchedule", JoinType.INNER);
            return cb.greaterThanOrEqualTo(schedule.get("basePrice"), minPrice);
        };
    }

    public static Specification<FlightInstance> hasPriceLessThanOrEqualTo(BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (maxPrice == null) {
                return cb.conjunction();
            }
            Join<FlightInstance, FlightSchedule> schedule = root.join("flightSchedule", JoinType.INNER);
            return cb.lessThanOrEqualTo(schedule.get("basePrice"), maxPrice);
        };
    }

    public static Specification<FlightInstance> departsOnDate(LocalDate date) {
        return (root, query, cb) -> date == null
                ? cb.conjunction()
                : cb.equal(root.get("departureDate"), date);
    }

    public static Specification<FlightInstance> hasMinimumAvailableSeats(Integer minSeats) {
        return (root, query, cb) -> {
            if (minSeats == null || minSeats <= 0) {
                return cb.conjunction();
            }

            Subquery<Long> capacity = query.subquery(Long.class);
            Root<Seat> seat = capacity.from(Seat.class);
            capacity.select(cb.count(seat))
                    .where(cb.equal(seat.get("flightInstance"), root));

            Subquery<Long> occupied = query.subquery(Long.class);
            Root<SeatReservation> reservation = occupied.from(SeatReservation.class);
            occupied.select(cb.count(reservation))
                    .where(cb.and(
                            cb.equal(reservation.get("seat").get("flightInstance"), root),
                            reservation.get("status").in(OCCUPYING_STATUSES)));

            return cb.greaterThanOrEqualTo(
                    cb.diff(capacity.getSelection(), occupied.getSelection()),
                    minSeats.longValue());
        };
    }
}