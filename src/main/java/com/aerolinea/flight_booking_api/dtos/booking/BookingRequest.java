package com.aerolinea.flight_booking_api.dtos.booking;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BookingRequest(
        @NotNull @Min(1) Integer numberOfPassengers,
        @NotEmpty @Valid List<ItineraryRequest> itineraries) {

    public record ItineraryRequest(
            @NotEmpty List<Long> flightInstanceIds) {
    }
}