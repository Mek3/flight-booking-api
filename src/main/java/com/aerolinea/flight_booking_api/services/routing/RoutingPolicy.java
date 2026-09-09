package com.aerolinea.flight_booking_api.services.routing;

import java.time.Duration;

public record RoutingPolicy(
        Duration minimumConnectionTime,
        Duration maximumLayoverTime,
        int maximumSegments) {

    public static RoutingPolicy defaults() {
        return new RoutingPolicy(Duration.ofMinutes(45), Duration.ofHours(24), 4);
    }
}
