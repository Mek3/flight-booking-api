package com.aerolinea.flight_booking_api.domain.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import com.aerolinea.flight_booking_api.services.routing.RoutingPolicy;
import com.aerolinea.flight_booking_api.services.routing.RoutingValidator;
import com.aerolinea.flight_booking_api.services.routing.SegmentTiming;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.aerolinea.flight_booking_api.exceptions.BusinessRuleViolationException;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;

class RoutingValidatorTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 10, 15, 8, 0);

    private final RoutingValidator validator = new RoutingValidator(RoutingPolicy.defaults());

    private SegmentTiming segment(int order, String from, String to, long departureOffsetMinutes, long durationMinutes) {
        LocalDateTime departure = BASE.plusMinutes(departureOffsetMinutes);
        return new SegmentTiming(order, from, to, departure, departure.plusMinutes(durationMinutes));
    }

    @Nested
    @DisplayName("Single segment itineraries")
    class SingleSegment {

        @Test
        @DisplayName("Should accept a direct flight without special-casing")
        void shouldAcceptDirectFlight() {
            List<SegmentTiming> segments = List.of(segment(1, "ALC", "BCN", 0, 75));

            assertThatCode(() -> validator.validate(segments)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should report no layovers for a direct flight")
        void shouldReportNoLayovers() {
            List<SegmentTiming> segments = List.of(segment(1, "ALC", "BCN", 0, 75));

            assertThat(validator.layovers(segments)).isEmpty();
        }

        @Test
        @DisplayName("Should reject a segment that arrives before it departs")
        void shouldRejectInvertedSegment() {
            SegmentTiming inverted = new SegmentTiming(1, "ALC", "BCN", BASE.plusHours(3), BASE);

            assertThatThrownBy(() -> validator.validate(List.of(inverted)))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEGMENT_ARRIVES_BEFORE_DEPARTURE);
        }
    }

    @Nested
    @DisplayName("Connecting itineraries")
    class Connections {

        @Test
        @DisplayName("Should accept a valid connection above the minimum connection time")
        void shouldAcceptValidConnection() {
            List<SegmentTiming> segments = List.of(
                    segment(1, "ALC", "MAD", 0, 60),
                    segment(2, "MAD", "JFK", 150, 480));

            assertThatCode(() -> validator.validate(segments)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reject a connection departing before the previous segment lands")
        void shouldRejectTemporalOverlap() {
            List<SegmentTiming> segments = List.of(
                    segment(1, "ALC", "MAD", 0, 120),
                    segment(2, "MAD", "JFK", 90, 480));

            assertThatThrownBy(() -> validator.validate(segments))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEGMENT_TEMPORAL_OVERLAP);
        }

        @Test
        @DisplayName("Should reject discontinuous airports")
        void shouldRejectAirportDiscontinuity() {
            List<SegmentTiming> segments = List.of(
                    segment(1, "ALC", "MAD", 0, 60),
                    segment(2, "BCN", "JFK", 150, 480));

            assertThatThrownBy(() -> validator.validate(segments))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEGMENT_AIRPORT_DISCONTINUITY);
        }

        @Test
        @DisplayName("Should reject a layover shorter than the minimum connection time")
        void shouldRejectSubMinimumLayover() {
            List<SegmentTiming> segments = List.of(
                    segment(1, "ALC", "MAD", 0, 60),
                    segment(2, "MAD", "JFK", 90, 480));

            assertThatThrownBy(() -> validator.validate(segments))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LAYOVER_BELOW_MINIMUM_CONNECTION_TIME);
        }

        @Test
        @DisplayName("Should accept a layover exactly equal to the minimum connection time")
        void shouldAcceptLayoverAtThreshold() {
            List<SegmentTiming> segments = List.of(
                    segment(1, "ALC", "MAD", 0, 60),
                    segment(2, "MAD", "JFK", 105, 480));

            assertThatCode(() -> validator.validate(segments)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reject a layover above the maximum allowed")
        void shouldRejectExcessiveLayover() {
            List<SegmentTiming> segments = List.of(
                    segment(1, "ALC", "MAD", 0, 60),
                    segment(2, "MAD", "JFK", 60 + 1500, 480));

            assertThatThrownBy(() -> validator.validate(segments))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LAYOVER_ABOVE_MAXIMUM);
        }

        @Test
        @DisplayName("Should match airports case-insensitively")
        void shouldMatchAirportsCaseInsensitively() {
            List<SegmentTiming> segments = List.of(
                    segment(1, "ALC", "mad", 0, 60),
                    segment(2, "MAD", "JFK", 150, 480));

            assertThatCode(() -> validator.validate(segments)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Layover and duration calculation")
    class Calculations {

        @Test
        @DisplayName("Should calculate one layover per connection")
        void shouldCalculateLayovers() {
            List<SegmentTiming> segments = List.of(
                    segment(1, "ALC", "MAD", 0, 60),
                    segment(2, "MAD", "CDG", 150, 120),
                    segment(3, "CDG", "JFK", 400, 480));

            assertThat(validator.layovers(segments))
                    .containsExactly(Duration.ofMinutes(90), Duration.ofMinutes(130));
        }

        @Test
        @DisplayName("Should measure total travel time from first departure to last arrival")
        void shouldCalculateTotalTravelTime() {
            List<SegmentTiming> segments = List.of(
                    segment(1, "ALC", "MAD", 0, 60),
                    segment(2, "MAD", "JFK", 150, 480));

            assertThat(validator.totalTravelTime(segments)).isEqualTo(Duration.ofMinutes(630));
        }

        @Test
        @DisplayName("Should handle overnight segments crossing midnight")
        void shouldHandleOvernightSegments() {
            LocalDateTime departure = LocalDateTime.of(2026, 10, 15, 23, 0);
            SegmentTiming overnight = new SegmentTiming(1, "MAD", "JFK", departure, departure.plusHours(9));

            assertThat(validator.totalTravelTime(List.of(overnight))).isEqualTo(Duration.ofHours(9));
            assertThatCode(() -> validator.validate(List.of(overnight))).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Itinerary boundaries")
    class Boundaries {

        @Test
        @DisplayName("Should reject an itinerary with no segments")
        void shouldRejectEmptyItinerary() {
            assertThatThrownBy(() -> validator.validate(List.of()))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ITINERARY_HAS_NO_SEGMENTS);
        }

        @Test
        @DisplayName("Should reject an itinerary exceeding the maximum segment count")
        void shouldRejectTooManySegments() {
            RoutingValidator strictValidator =
                    new RoutingValidator(new RoutingPolicy(Duration.ofMinutes(45), Duration.ofHours(24), 2));

            List<SegmentTiming> segments = List.of(
                    segment(1, "ALC", "MAD", 0, 60),
                    segment(2, "MAD", "CDG", 150, 120),
                    segment(3, "CDG", "JFK", 400, 480));

            assertThatThrownBy(() -> strictValidator.validate(segments))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ITINERARY_TOO_MANY_SEGMENTS);
        }
    }
}