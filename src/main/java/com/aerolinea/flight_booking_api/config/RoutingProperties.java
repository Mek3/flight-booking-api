package com.aerolinea.flight_booking_api.config;

import java.time.Duration;

import com.aerolinea.flight_booking_api.services.routing.RoutingPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "app.routing")
public record RoutingProperties(
        Duration minimumConnectionTime,
        Duration maximumLayoverTime,
        Integer maximumSegments) {

    public RoutingPolicy toPolicy() {
        return new RoutingPolicy(
                minimumConnectionTime != null ? minimumConnectionTime : Duration.ofMinutes(45),
                maximumLayoverTime != null ? maximumLayoverTime : Duration.ofHours(24),
                maximumSegments != null ? maximumSegments : 4);
    }
}