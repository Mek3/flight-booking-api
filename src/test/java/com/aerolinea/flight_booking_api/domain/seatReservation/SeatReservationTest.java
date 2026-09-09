package com.aerolinea.flight_booking_api.domain.seatReservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.aerolinea.flight_booking_api.exceptions.BusinessRuleViolationException;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.aerolinea.flight_booking_api.models.SeatReservation;
import com.aerolinea.flight_booking_api.models.enums.SeatReservationStatus;

class SeatReservationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 15, 12, 0);

    private SeatReservation newHold() {
        return SeatReservation.builder()
                .heldUntil(NOW.plusMinutes(15))
                .build();
    }

    @Nested
    @DisplayName("Lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("Should open in HELD")
        void shouldOpenHeld() {
            assertThat(newHold().getStatus()).isEqualTo(SeatReservationStatus.HELD);
        }

        @Test
        @DisplayName("Should move from HELD to CONFIRMED")
        void shouldConfirm() {
            SeatReservation hold = newHold();

            hold.confirm();

            assertThat(hold.getStatus()).isEqualTo(SeatReservationStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Should move from HELD to EXPIRED")
        void shouldExpire() {
            SeatReservation hold = newHold();

            hold.expire();

            assertThat(hold.getStatus()).isEqualTo(SeatReservationStatus.EXPIRED);
        }
    }

    @Nested
    @DisplayName("Invalid transitions")
    class InvalidTransitions {

        @Test
        @DisplayName("Should reject expiring a confirmed seat")
        void shouldRejectExpiringConfirmed() {
            SeatReservation hold = newHold();
            hold.confirm();

            assertThatThrownBy(hold::expire)
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_RESERVATION_INVALID_TRANSITION);
        }

        @Test
        @DisplayName("Should reject confirming an expired seat")
        void shouldRejectConfirmingExpired() {
            SeatReservation hold = newHold();
            hold.expire();

            assertThatThrownBy(hold::confirm)
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_RESERVATION_INVALID_TRANSITION);
        }

        @Test
        @DisplayName("Should reject confirming twice")
        void shouldRejectDoubleConfirm() {
            SeatReservation hold = newHold();
            hold.confirm();

            assertThatThrownBy(hold::confirm)
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_RESERVATION_INVALID_TRANSITION);
        }
    }

    @Nested
    @DisplayName("Hold expiry window")
    class Expiry {

        @Test
        @DisplayName("Should report a hold as expired once its window has passed")
        void shouldDetectExpiredHold() {
            SeatReservation hold = newHold();

            assertThat(hold.isExpiredAt(NOW.plusMinutes(16))).isTrue();
        }

        @Test
        @DisplayName("Should not report a hold as expired within its window")
        void shouldNotDetectLiveHold() {
            SeatReservation hold = newHold();

            assertThat(hold.isExpiredAt(NOW.plusMinutes(14))).isFalse();
        }

        @Test
        @DisplayName("Should never report a confirmed seat as expired, however old the hold")
        void shouldNeverExpireConfirmed() {
            SeatReservation hold = newHold();
            hold.confirm();

            assertThat(hold.isExpiredAt(NOW.plusDays(30))).isFalse();
        }
    }

    @Nested
    @DisplayName("Seat occupancy")
    class Occupancy {

        @Test
        @DisplayName("Should treat HELD and CONFIRMED as occupying the seat")
        void shouldOccupy() {
            assertThat(SeatReservationStatus.HELD.occupiesSeat()).isTrue();
            assertThat(SeatReservationStatus.CONFIRMED.occupiesSeat()).isTrue();
        }

        @Test
        @DisplayName("Should release the seat once expired")
        void shouldRelease() {
            assertThat(SeatReservationStatus.EXPIRED.occupiesSeat()).isFalse();
        }
    }

    @Test
    @DisplayName("Should allow a fresh hold on a seat whose previous hold expired")
    void shouldAllowRehold() {
        SeatReservation first = newHold();
        first.expire();

        assertThatCode(() -> SeatReservation.builder().heldUntil(NOW.plusMinutes(15)).build())
                .doesNotThrowAnyException();
    }
}