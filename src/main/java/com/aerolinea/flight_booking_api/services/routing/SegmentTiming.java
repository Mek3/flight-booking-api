package com.aerolinea.flight_booking_api.services.routing;

import java.time.Duration;
import java.time.LocalDateTime;

public record SegmentTiming(
        int order,
        String departureAirport,
        String arrivalAirport,
        LocalDateTime departureAt,
        LocalDateTime arrivalAt) {

    public Duration duration() {
        return Duration.between(departureAt, arrivalAt);
    }

    public boolean connectsTo(SegmentTiming next) {
        return this.arrivalAirport.equalsIgnoreCase(next.departureAirport());
    }

    public Duration layoverBefore(SegmentTiming next) {
        return Duration.between(this.arrivalAt, next.departureAt());
    }
}
