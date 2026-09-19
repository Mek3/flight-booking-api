package com.aerolinea.flight_booking_api.domain.reservation;

import com.aerolinea.flight_booking_api.mappers.ReservationMapper;
import com.aerolinea.flight_booking_api.repositories.ReservationRepository;
import com.aerolinea.flight_booking_api.services.ReservationService;
import com.aerolinea.flight_booking_api.services.ReservationServiceImpl;
import com.aerolinea.flight_booking_api.services.SeatReservationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SweepConflictTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SeatReservationService seatReservationService;

    @Mock
    private ReservationMapper reservationMapper;

    @Mock
    private ReservationService selfProxy;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reservationService, "reservationService", selfProxy);
    }

    @Test
    @DisplayName("Should keep sweeping when one hold is resolved by someone else first")
    void shouldContinueAfterOptimisticConflict() {
        when(reservationRepository.findExpiredReservationIds(any(LocalDateTime.class)))
                .thenReturn(List.of(1L, 2L, 3L));
        doThrow(new OptimisticLockingFailureException("hold changed"))
                .when(selfProxy).processSingleExpiration(2L);

        assertThatCode(() -> reservationService.expirePendingReservations())
                .as("a hold resolved concurrently must not abort the batch")
                .doesNotThrowAnyException();

        verify(selfProxy).processSingleExpiration(1L);
        verify(selfProxy).processSingleExpiration(3L);
    }

    @Test
    @DisplayName("Should keep sweeping when one reservation fails for any other reason")
    void shouldContinueAfterGenericFailure() {
        when(reservationRepository.findExpiredReservationIds(any(LocalDateTime.class)))
                .thenReturn(List.of(1L, 2L, 3L));
        doThrow(new IllegalStateException("boom"))
                .when(selfProxy).processSingleExpiration(2L);

        assertThatCode(() -> reservationService.expirePendingReservations())
                .doesNotThrowAnyException();

        verify(selfProxy).processSingleExpiration(1L);
        verify(selfProxy).processSingleExpiration(3L);
    }

    @Test
    @DisplayName("Should not reach the sweep body when nothing has expired")
    void shouldSkipEmptySweep() {
        when(reservationRepository.findExpiredReservationIds(any(LocalDateTime.class)))
                .thenReturn(List.of());

        reservationService.expirePendingReservations();

        verify(selfProxy, org.mockito.Mockito.never()).processSingleExpiration(anyLong());
    }
}