package com.aerolinea.flight_booking_api.services.routing;

import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Service;

import com.aerolinea.flight_booking_api.models.FlightSegment;
import com.aerolinea.flight_booking_api.models.Itinerary;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ItineraryRoutingService {

    private final RoutingValidator routingValidator;

    public void validate(Itinerary itinerary) {
        routingValidator.validate(toTimings(itinerary));
    }

    public List<Duration> layovers(Itinerary itinerary) {
        return routingValidator.layovers(toTimings(itinerary));
    }

    public Duration totalTravelTime(Itinerary itinerary) {
        return routingValidator.totalTravelTime(toTimings(itinerary));
    }

    private List<SegmentTiming> toTimings(Itinerary itinerary) {
        return itinerary.getSegments().stream()
                .map(this::toTiming)
                .toList();
    }

    private SegmentTiming toTiming(FlightSegment segment) {
        return new SegmentTiming(
                segment.getSegmentOrder(),
                segment.getDepartureAirport().getCode(),
                segment.getArrivalAirport().getCode(),
                segment.getDepartureAt(),
                segment.getArrivalAt());
    }
}