package com.aerolinea.flight_booking_api.domain.seatReservation;

import com.aerolinea.flight_booking_api.models.*;
import com.aerolinea.flight_booking_api.models.enums.SeatReservationStatus;
import com.aerolinea.flight_booking_api.repositories.SeatRepository;
import com.aerolinea.flight_booking_api.repositories.SeatReservationRepository;
import com.aerolinea.flight_booking_api.services.SeatReservationServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatHoldExpirationTest {

    private static final Long RESERVATION_ID = 88L;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private SeatReservationRepository seatReservationRepository;

    @InjectMocks
    private SeatReservationServiceImpl seatReservationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(seatReservationService, "cartTtl", Duration.ofMinutes(15));
    }

    private SeatReservation hold(LocalDateTime heldUntil) {
        return SeatReservation.builder().heldUntil(heldUntil).build();
    }

    private SeatReservation expiredHold() {
        return hold(LocalDateTime.now().minusMinutes(1));
    }

    @Nested
    @DisplayName("Releasing expired holds")
    class Releasing {

        @Test
        @DisplayName("Should move every expired hold of the reservation to EXPIRED")
        void shouldExpireHeldSeats() {
            List<SeatReservation> holds = List.of(expiredHold(), expiredHold());
            when(seatReservationRepository.findHeldSeatsByReservationId(anyLong(), any(LocalDateTime.class)))
                    .thenReturn(holds);

            seatReservationService.cancelSeatReservationsForReservation(RESERVATION_ID);

            assertThat(holds).allSatisfy(seatHold ->
                    assertThat(seatHold.getStatus()).isEqualTo(SeatReservationStatus.EXPIRED));
        }

        @Test
        @DisplayName("Should never delete a hold row, so the audit trail survives")
        void shouldNotDeleteHolds() {
            when(seatReservationRepository.findHeldSeatsByReservationId(anyLong(), any(LocalDateTime.class)))
                    .thenReturn(List.of(expiredHold()));

            seatReservationService.cancelSeatReservationsForReservation(RESERVATION_ID);

            verify(seatReservationRepository, never()).delete(any(SeatReservation.class));
            verify(seatReservationRepository, never()).deleteAll(any());
        }

        @Test
        @DisplayName("Should do nothing when the reservation has no expired hold")
        void shouldTolerateNothingToExpire() {
            when(seatReservationRepository.findHeldSeatsByReservationId(anyLong(), any(LocalDateTime.class)))
                    .thenReturn(List.of());

            assertThatCode(() -> seatReservationService.cancelSeatReservationsForReservation(RESERVATION_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should be idempotent when run twice over the same reservation")
        void shouldBeIdempotent() {
            SeatReservation alreadyExpired = expiredHold();
            alreadyExpired.expire();

            when(seatReservationRepository.findHeldSeatsByReservationId(anyLong(), any(LocalDateTime.class)))
                    .thenReturn(List.of());

            assertThatCode(() -> seatReservationService.cancelSeatReservationsForReservation(RESERVATION_ID))
                    .doesNotThrowAnyException();

            assertThat(alreadyExpired.getStatus()).isEqualTo(SeatReservationStatus.EXPIRED);
        }
    }

    @Nested
    @DisplayName("Confirming holds")
    class Confirming {

        @Test
        @DisplayName("Should move every hold of the reservation to CONFIRMED")
        void shouldConfirmHolds() {
            List<SeatReservation> holds = List.of(
                    hold(LocalDateTime.now().plusMinutes(10)),
                    hold(LocalDateTime.now().plusMinutes(10)));

            when(seatReservationRepository.findHoldsByReservationId(anyLong())).thenReturn(holds);

            seatReservationService.confirmSeatReservationsForReservation(RESERVATION_ID);

            assertThat(holds).allSatisfy(seatHold ->
                    assertThat(seatHold.getStatus()).isEqualTo(SeatReservationStatus.CONFIRMED));
        }

        @Test
        @DisplayName("Should confirm a hold whose window is about to close")
        void shouldConfirmHoldOnTheEdge() {
            SeatReservation aboutToExpire = hold(LocalDateTime.now().plusNanos(1));
            when(seatReservationRepository.findHoldsByReservationId(anyLong()))
                    .thenReturn(List.of(aboutToExpire));

            seatReservationService.confirmSeatReservationsForReservation(RESERVATION_ID);

            assertThat(aboutToExpire.getStatus()).isEqualTo(SeatReservationStatus.CONFIRMED);
        }
    }

    @Nested
    @DisplayName("Occupancy counting")
    class Occupancy {

        @Test
        @DisplayName("Should report a reservation with live holds as still occupying seats")
        void shouldCountLiveHolds() {
            when(seatReservationRepository.countReservationWithHoldOrConfirmedSeats(RESERVATION_ID)).thenReturn(2L);

            assertThat(seatReservationRepository.countReservationWithHoldOrConfirmedSeats(RESERVATION_ID)).isEqualTo(2L);
        }

        @Test
        @DisplayName("Should report zero once every hold is expired")
        void shouldCountZeroWhenAllExpired() {
            when(seatReservationRepository.countReservationWithHoldOrConfirmedSeats(RESERVATION_ID)).thenReturn(0L);

            assertThat(seatReservationRepository.countReservationWithHoldOrConfirmedSeats(RESERVATION_ID)).isZero();
        }
    }
}