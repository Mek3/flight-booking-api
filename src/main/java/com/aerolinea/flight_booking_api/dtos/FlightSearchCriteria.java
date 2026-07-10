package com.aerolinea.flight_booking_api.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

public record FlightSearchCriteria(
    String departure,
    String destination,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    Integer minAvailableSeats,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate date
) {}