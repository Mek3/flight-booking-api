package com.aerolinea.flight_booking_api.services.routing;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.aerolinea.flight_booking_api.exceptions.BusinessRuleViolationException;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;

public class RoutingValidator {

    private final RoutingPolicy policy;

    public RoutingValidator(RoutingPolicy policy) {
        this.policy = policy;
    }

    public void validate(List<SegmentTiming> segments) {
        if (segments == null || segments.isEmpty()) {
            throw new BusinessRuleViolationException(ErrorCode.ITINERARY_HAS_NO_SEGMENTS,
                    ErrorCode.ITINERARY_HAS_NO_SEGMENTS.getMessage());
        }

        if (segments.size() > policy.maximumSegments()) {
            throw new BusinessRuleViolationException(ErrorCode.ITINERARY_TOO_MANY_SEGMENTS,
                    String.format(ErrorCode.ITINERARY_TOO_MANY_SEGMENTS.getMessage(),
                            segments.size(), policy.maximumSegments()));
        }

        for (SegmentTiming segment : segments) {
            if (!segment.arrivalAt().isAfter(segment.departureAt())) {
                throw new BusinessRuleViolationException(ErrorCode.SEGMENT_ARRIVES_BEFORE_DEPARTURE,
                        String.format(ErrorCode.SEGMENT_ARRIVES_BEFORE_DEPARTURE.getMessage(), segment.order()));
            }
        }

        for (int i = 0; i < segments.size() - 1; i++) {
            SegmentTiming current = segments.get(i);
            SegmentTiming next = segments.get(i + 1);

            if (!current.connectsTo(next)) {
                throw new BusinessRuleViolationException(ErrorCode.SEGMENT_AIRPORT_DISCONTINUITY,
                        String.format(ErrorCode.SEGMENT_AIRPORT_DISCONTINUITY.getMessage(),
                                current.order(), current.arrivalAirport(),
                                next.order(), next.departureAirport()));
            }

            Duration layover = current.layoverBefore(next);

            if (layover.isNegative() || layover.isZero()) {
                throw new BusinessRuleViolationException(ErrorCode.SEGMENT_TEMPORAL_OVERLAP,
                        String.format(ErrorCode.SEGMENT_TEMPORAL_OVERLAP.getMessage(),
                                next.order(), current.order()));
            }

            if (layover.compareTo(policy.minimumConnectionTime()) < 0) {
                throw new BusinessRuleViolationException(ErrorCode.LAYOVER_BELOW_MINIMUM_CONNECTION_TIME,
                        String.format(ErrorCode.LAYOVER_BELOW_MINIMUM_CONNECTION_TIME.getMessage(),
                                layover.toMinutes(), policy.minimumConnectionTime().toMinutes(),
                                current.arrivalAirport()));
            }

            if (layover.compareTo(policy.maximumLayoverTime()) > 0) {
                throw new BusinessRuleViolationException(ErrorCode.LAYOVER_ABOVE_MAXIMUM,
                        String.format(ErrorCode.LAYOVER_ABOVE_MAXIMUM.getMessage(),
                                layover.toHours(), policy.maximumLayoverTime().toHours()));
            }
        }
    }

    public List<Duration> layovers(List<SegmentTiming> segments) {
        List<Duration> layovers = new ArrayList<>();

        for (int i = 0; i < segments.size() - 1; i++) {
            layovers.add(segments.get(i).layoverBefore(segments.get(i + 1)));
        }

        return layovers;
    }

    public Duration totalTravelTime(List<SegmentTiming> segments) {
        SegmentTiming first = segments.get(0);
        SegmentTiming last = segments.get(segments.size() - 1);
        return Duration.between(first.departureAt(), last.arrivalAt());
    }
}
