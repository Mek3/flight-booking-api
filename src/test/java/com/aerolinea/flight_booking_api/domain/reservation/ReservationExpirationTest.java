package com.aerolinea.flight_booking_api.domain.reservation;

import com.aerolinea.flight_booking_api.exceptions.ResourceNotFoundException;
import com.aerolinea.flight_booking_api.mappers.BookingMapper;
import com.aerolinea.flight_booking_api.models.Reservation;
import com.aerolinea.flight_booking_api.models.enums.ReservationStatus;
import com.aerolinea.flight_booking_api.repositories.ReservationRepository;
import com.aerolinea.flight_booking_api.repositories.SeatReservationRepository;
import com.aerolinea.flight_booking_api.services.ReservationService;
import com.aerolinea.flight_booking_api.services.ReservationServiceImpl;
import com.aerolinea.flight_booking_api.services.SeatReservationService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationExpirationTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SeatReservationService seatReservationService;

    @Mock
    private SeatReservationRepository seatReservationRepository;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private ReservationService selfProxy;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private Reservation pendingReservation(Long id) {
        Reservation reservation = Reservation.builder()
                .reservationCode("CODE" + id)
                .status(ReservationStatus.PENDING)
                .numberOfPassengers(1)
                .totalPrice(BigDecimal.TEN)
                .build();

        ReflectionTestUtils.setField(reservation, "id", id);
        return reservation;
    }

    private void injectSelfProxy() {
        ReflectionTestUtils.setField(reservationService, "reservationService", selfProxy);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Sweeping expired reservations")
    class Sweep {

        @Test
        @DisplayName("Should process every reservation holding an expired seat")
        void shouldProcessEachExpiredReservation() {
            injectSelfProxy();
            when(reservationRepository.findExpiredReservationIds(any(LocalDateTime.class)))
                    .thenReturn(List.of(1L, 2L, 3L));

            reservationService.expirePendingReservations();

            verify(selfProxy, times(3)).processSingleExpiration(anyLong());
        }

        @Test
        @DisplayName("Should not touch the database when nothing has expired")
        void shouldSkipEmptySweep() {
            injectSelfProxy();
            when(reservationRepository.findExpiredReservationIds(any(LocalDateTime.class)))
                    .thenReturn(List.of());

            reservationService.expirePendingReservations();

            verify(selfProxy, never()).processSingleExpiration(anyLong());
        }

        @Test
        @DisplayName("Should keep sweeping after one reservation fails")
        void shouldIsolateFailures() {
            injectSelfProxy();
            when(reservationRepository.findExpiredReservationIds(any(LocalDateTime.class)))
                    .thenReturn(List.of(1L, 2L, 3L));
            doThrow(new IllegalStateException("boom")).when(selfProxy).processSingleExpiration(2L);

            assertThatCode(() -> reservationService.expirePendingReservations()).doesNotThrowAnyException();

            verify(selfProxy).processSingleExpiration(1L);
            verify(selfProxy).processSingleExpiration(3L);
        }
    }

    @Nested
    @DisplayName("Expiring a single reservation")
    class SingleExpiration {

        @Test
        @DisplayName("Should release the held seats before deciding on the reservation")
        void shouldReleaseSeatsFirst() {
            Reservation reservation = pendingReservation(1L);
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
            when(seatReservationRepository.countReservationWithHoldOrConfirmedSeats(1L)).thenReturn(0L);

            reservationService.processSingleExpiration(1L);

            verify(seatReservationService).cancelSeatReservationsForReservation(1L);
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        }

        @Test
        @DisplayName("Should leave the reservation alone while a seat is still confirmed or held")
        void shouldNotExpireWhileSeatsRemain() {
            Reservation reservation = pendingReservation(1L);
            when(seatReservationRepository.countReservationWithHoldOrConfirmedSeats(1L)).thenReturn(1L);

            reservationService.processSingleExpiration(1L);

            assertThat(reservation.getStatus())
                    .as("a booking confirmed moments before the sweep must survive it")
                    .isEqualTo(ReservationStatus.PENDING);
        }

        @Test
        @DisplayName("Should fail loudly when the reservation no longer exists")
        void shouldRejectMissingReservation() {
            when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatCode(() -> reservationService.processSingleExpiration(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Confirming a reservation")
    class Confirmation {

        @Test
        @DisplayName("Should confirm the seats along with the reservation")
        void shouldConfirmSeatsToo() {
            Reservation reservation = pendingReservation(1L);
            ReflectionTestUtils.setField(reservationService, "reservationService", selfProxy);
            when(reservationRepository.findByIdAndUserUsername(anyLong(), any())).thenReturn(Optional.of(reservation));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken("pacog", null, List.of()));
            SecurityContextHolder.setContext(context);

            reservationService.confirmReservation(1L);

            verify(seatReservationService).confirmSeatReservationsForReservation(1L);
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        }
    }
}