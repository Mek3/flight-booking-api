package com.aerolinea.flight_booking_api.specifications;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.data.jpa.domain.Specification;

import com.aerolinea.flight_booking_api.models.Flight;

public class FlightSpecification {

    private FlightSpecification() {}

    public static Specification<Flight> hasDeparture(String departure) {
        return (root, query, cb) -> departure == null || departure.isBlank() 
                ? cb.conjunction() 
                : cb.equal(root.get("departure"), departure);
    }

    public static Specification<Flight> hasDestination(String destination) {
        return (root, query, cb) -> destination == null || destination.isBlank() 
                ? cb.conjunction() 
                : cb.equal(root.get("destination"), destination);
    }

    public static Specification<Flight> hasPriceGreaterThanOrEqualTo(BigDecimal minPrice) {
        return (root, query, cb) -> minPrice == null 
                ? cb.conjunction() 
                : cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Flight> hasPriceLessThanOrEqualTo(BigDecimal maxPrice) {
        return (root, query, cb) -> maxPrice == null 
                ? cb.conjunction() 
                : cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    public static Specification<Flight> hasMinimumAvailableSeats(Integer minSeats) {
        return (root, query, cb) -> minSeats == null || minSeats <= 0
                ? cb.conjunction() 
                : cb.greaterThanOrEqualTo(root.get("availableSeats"), minSeats);
    }

    public static Specification<Flight> departsOnDate(LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) return cb.conjunction();
            
            return cb.between(root.get("departureTime"), 
                              date.atStartOfDay(), 
                              date.atTime(LocalTime.MAX));
        };
    }
}