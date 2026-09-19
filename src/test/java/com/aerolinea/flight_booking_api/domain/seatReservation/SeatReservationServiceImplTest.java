package com.aerolinea.flight_booking_api.domain.seatReservation;

import com.aerolinea.flight_booking_api.dtos.booking.BookingRequest;
import com.aerolinea.flight_booking_api.exceptions.BusinessRuleViolationException;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.aerolinea.flight_booking_api.exceptions.ResourceNotFoundException;
import com.aerolinea.flight_booking_api.models.*;
import com.aerolinea.flight_booking_api.models.enums.FlightStatus;
import com.aerolinea.flight_booking_api.models.enums.ReservationStatus;
import com.aerolinea.flight_booking_api.models.enums.SeatReservationStatus;
import com.aerolinea.flight_booking_api.repositories.SeatRepository;
import com.aerolinea.flight_booking_api.repositories.SeatReservationRepository;
import com.aerolinea.flight_booking_api.services.SeatReservationServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatReservationServiceImplTest {

    private static final Duration CART_TTL = Duration.ofMinutes(15);
    private static final Long FLIGHT_A = 101L;
    private static final Long FLIGHT_B = 205L;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private SeatReservationRepository seatReservationRepository;

    @InjectMocks
    private SeatReservationServiceImpl seatReservationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(seatReservationService, "cartTtl", CART_TTL);
    }

    private BookingRequest.FlightSegmentRequest segment(Long flightInstanceId, Long... seatIds) {
        return new BookingRequest.FlightSegmentRequest(flightInstanceId, List.of(seatIds));
    }

    private FlightInstance flightInstance(Long id) {
        FlightSchedule schedule = FlightSchedule.builder()
                .flightNumber("IBE-" + id)
                .departureTime(LocalTime.of(8, 0))
                .arrivalTime(LocalTime.of(10, 0))
                .arrivalDayOffset(0)
                .daysOfWeekMask(127)
                .basePrice(new BigDecimal("100.00"))
                .build();

        FlightInstance instance = FlightInstance.builder()
                .flightSchedule(schedule)
                .departureDate(LocalDate.of(2026, 10, 15))
                .status(FlightStatus.SCHEDULED)
                .build();

        ReflectionTestUtils.setField(instance, "id", id);
        return instance;
    }

    private Seat seat(Long id, FlightInstance instance) {
        Seat seat = Seat.builder()
                .flightInstance(instance)
                .rowNumber(1)
                .seatLetter("A")
                .build();

        ReflectionTestUtils.setField(seat, "id", id);
        return seat;
    }

    private SeatReservation existingHold(SeatReservationStatus status) {
        SeatReservation hold = SeatReservation.builder()
                .heldUntil(LocalDateTime.now().plusMinutes(10))
                .build();

        ReflectionTestUtils.setField(hold, "status", status);
        return hold;
    }

    @Nested
    @DisplayName("Seat selection")
    class SeatSelection {

        @Test
        @DisplayName("Should accept one seat per passenger on every segment")
        void shouldAcceptMatchingCounts() {
            List<BookingRequest.FlightSegmentRequest> segments = List.of(
                    segment(FLIGHT_A, 1L, 2L),
                    segment(FLIGHT_B, 3L, 4L));

            assertThatCode(() -> seatReservationService.validateSeatSelection(segments, 2))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reject fewer seats than passengers")
        void shouldRejectTooFewSeats() {
            List<BookingRequest.FlightSegmentRequest> segments = List.of(segment(FLIGHT_A, 1L));

            assertThatThrownBy(() -> seatReservationService.validateSeatSelection(segments, 2))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_COUNT_MISMATCH);
        }

        @Test
        @DisplayName("Should reject more seats than passengers")
        void shouldRejectTooManySeats() {
            List<BookingRequest.FlightSegmentRequest> segments = List.of(segment(FLIGHT_A, 1L, 2L, 3L));

            assertThatThrownBy(() -> seatReservationService.validateSeatSelection(segments, 2))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_COUNT_MISMATCH);
        }

        @Test
        @DisplayName("Should reject the same seat twice in one segment")
        void shouldRejectDuplicateSeat() {
            List<BookingRequest.FlightSegmentRequest> segments = List.of(segment(FLIGHT_A, 5L, 5L));

            assertThatThrownBy(() -> seatReservationService.validateSeatSelection(segments, 2))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_SEAT_IN_SEGMENT);
        }


        @Test
        @DisplayName("Should report the duplicate rather than the count it causes")
        void shouldPreferDuplicateErrorOverCountError() {
            List<BookingRequest.FlightSegmentRequest> segments = List.of(segment(FLIGHT_A, 5L, 5L));

            assertThatThrownBy(() -> seatReservationService.validateSeatSelection(segments, 3))
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_SEAT_IN_SEGMENT);
        }

        @Test
        @DisplayName("Should allow the same seat number on different segments")
        void shouldAllowSameSeatOnDifferentSegments() {
            List<BookingRequest.FlightSegmentRequest> segments = List.of(
                    segment(FLIGHT_A, 1L),
                    segment(FLIGHT_B, 1L));

            assertThatCode(() -> seatReservationService.validateSeatSelection(segments, 1))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Seats belong to their flight")
    class SeatOwnership {

        @Test
        @DisplayName("Should accept seats that all belong to the requested flight")
        void shouldAcceptSeatsOnFlight() {
            when(seatRepository.countSeatsByFlightInstanceIdAndSeatIds(FLIGHT_A, List.of(1L, 2L))).thenReturn(2L);

            assertThatCode(() -> seatReservationService.validateSeatsBelongToFlights(
                    List.of(segment(FLIGHT_A, 1L, 2L)))).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reject a seat that belongs to another flight")
        void shouldRejectSeatOnAnotherFlight() {
            when(seatRepository.countSeatsByFlightInstanceIdAndSeatIds(anyLong(), anyList())).thenReturn(1L);

            assertThatThrownBy(() -> seatReservationService.validateSeatsBelongToFlights(
                    List.of(segment(FLIGHT_A, 1L, 999L))))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_NOT_ON_SEGMENT_FLIGHT);
        }

        @Test
        @DisplayName("Should check every segment, not only the first")
        void shouldCheckAllSegments() {
            when(seatRepository.countSeatsByFlightInstanceIdAndSeatIds(FLIGHT_A, List.of(1L))).thenReturn(1L);
            when(seatRepository.countSeatsByFlightInstanceIdAndSeatIds(FLIGHT_B, List.of(2L))).thenReturn(0L);

            assertThatThrownBy(() -> seatReservationService.validateSeatsBelongToFlights(
                    List.of(segment(FLIGHT_A, 1L), segment(FLIGHT_B, 2L))))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Lock acquisition and availability")
    class LockAcquisition {

        @Test
        @DisplayName("Should return the locked seats when none is taken")
        void shouldReturnLockedSeats() {
            FlightInstance instance = flightInstance(FLIGHT_A);
            when(seatRepository.lockSeats(List.of(1L, 2L)))
                    .thenReturn(List.of(seat(1L, instance), seat(2L, instance)));
            when(seatReservationRepository.findBySeatIdIn(List.of(1L, 2L))).thenReturn(List.of());

            List<Seat> locked = seatReservationService.acquireSeatLocksAndValidate(List.of(1L, 2L));

            assertThat(locked).hasSize(2);
        }

        @Test
        @DisplayName("Should reject a seat id that does not exist")
        void shouldRejectMissingSeat() {
            FlightInstance instance = flightInstance(FLIGHT_A);
            when(seatRepository.lockSeats(List.of(1L, 999L))).thenReturn(List.of(seat(1L, instance)));

            assertThatThrownBy(() -> seatReservationService.acquireSeatLocksAndValidate(List.of(1L, 999L)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_NOT_FOUND);
        }

        @Test
        @DisplayName("Should report a sold seat as permanently unavailable")
        void shouldReportConfirmedSeatAsSold() {
            FlightInstance instance = flightInstance(FLIGHT_A);
            when(seatRepository.lockSeats(List.of(1L))).thenReturn(List.of(seat(1L, instance)));
            when(seatReservationRepository.findBySeatIdIn(List.of(1L)))
                    .thenReturn(List.of(existingHold(SeatReservationStatus.CONFIRMED)));

            assertThatThrownBy(() -> seatReservationService.acquireSeatLocksAndValidate(List.of(1L)))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting(ex -> ((BusinessRuleViolationException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.SEAT_ALREADY_BOOKED);
        }

        @Test
        @DisplayName("Should report a held seat as a temporary conflict")
        void shouldReportHeldSeatAsConflict() {
            FlightInstance instance = flightInstance(FLIGHT_A);
            when(seatRepository.lockSeats(List.of(1L))).thenReturn(List.of(seat(1L, instance)));
            when(seatReservationRepository.findBySeatIdIn(List.of(1L)))
                    .thenReturn(List.of(existingHold(SeatReservationStatus.HELD)));

            assertThatThrownBy(() -> seatReservationService.acquireSeatLocksAndValidate(List.of(1L)))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_CURRENTLY_LOCKED);
        }

        @Test
        @DisplayName("Should prefer the sold error when one seat is sold and another is held")
        void shouldPreferSoldOverHeld() {
            FlightInstance instance = flightInstance(FLIGHT_A);
            when(seatRepository.lockSeats(List.of(1L, 2L)))
                    .thenReturn(List.of(seat(1L, instance), seat(2L, instance)));
            when(seatReservationRepository.findBySeatIdIn(List.of(1L, 2L))).thenReturn(List.of(
                    existingHold(SeatReservationStatus.HELD),
                    existingHold(SeatReservationStatus.CONFIRMED)));


            assertThatThrownBy(() -> seatReservationService.acquireSeatLocksAndValidate(List.of(1L, 2L)))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting(ex -> ((BusinessRuleViolationException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.SEAT_ALREADY_BOOKED);
        }

        @Test
        @DisplayName("Should ignore an expired reservation on the same seat")
        void shouldIgnoreExpiredReservation() {
            FlightInstance instance = flightInstance(FLIGHT_A);
            when(seatRepository.lockSeats(List.of(1L))).thenReturn(List.of(seat(1L, instance)));
            when(seatReservationRepository.findBySeatIdIn(List.of(1L)))
                    .thenReturn(List.of(existingHold(SeatReservationStatus.EXPIRED)));

            assertThatCode(() -> seatReservationService.acquireSeatLocksAndValidate(List.of(1L)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should translate a lock failure into a retryable conflict")
        void shouldTranslateLockFailure() {
            when(seatRepository.lockSeats(List.of(1L)))
                    .thenThrow(new CannotAcquireLockException("lock wait timeout"));

            assertThatThrownBy(() -> seatReservationService.acquireSeatLocksAndValidate(List.of(1L)))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEAT_CURRENTLY_LOCKED);
        }
    }

    @Nested
    @DisplayName("Hold creation")
    class HoldCreation {

        private Reservation reservationWith(FlightInstance... instances) {
            Reservation reservation = Reservation.builder()
                    .reservationCode("TEST1234")
                    .status(ReservationStatus.PENDING)
                    .numberOfPassengers(1)
                    .totalPrice(BigDecimal.TEN)
                    .build();

            Itinerary itinerary = Itinerary.builder().build();

            for (FlightInstance instance : instances) {
                FlightSegment flightSegment = FlightSegment.builder().flightInstance(instance).build();
                ReflectionTestUtils.setField(flightSegment, "id", instance.getId() * 10);
                itinerary.addSegment(flightSegment);
            }

            reservation.addItinerary(itinerary);
            return reservation;
        }

        @SuppressWarnings("unchecked")
        private List<SeatReservation> capturedHolds() {
            ArgumentCaptor<List<SeatReservation>> captor = ArgumentCaptor.forClass(List.class);
            verify(seatReservationRepository).saveAll(captor.capture());
            return captor.getValue();
        }

        @Test
        @DisplayName("Should create one hold per seat on a single-segment booking")
        void shouldCreateOneHoldPerSeat() {
            FlightInstance instance = flightInstance(FLIGHT_A);
            Reservation reservation = reservationWith(instance);
            List<Seat> seats = List.of(seat(1L, instance), seat(2L, instance));

            seatReservationService.createHoldsForReservation(reservation, seats);

            assertThat(capturedHolds()).hasSize(2);
        }

        @Test
        @DisplayName("Should assign each seat to the segment flying its own aircraft")
        void shouldMatchSeatsToTheirSegment() {
            FlightInstance first = flightInstance(FLIGHT_A);
            FlightInstance second = flightInstance(FLIGHT_B);
            Reservation reservation = reservationWith(first, second);
            List<Seat> seats = List.of(seat(1L, first), seat(2L, second));

            seatReservationService.createHoldsForReservation(reservation, seats);

            List<SeatReservation> holds = capturedHolds();

            assertThat(holds).hasSize(2);
            assertThat(holds).allSatisfy(hold -> assertThat(hold.getSeat().getFlightInstance().getId())
                    .isEqualTo(hold.getFlightSegment().getFlightInstance().getId()));
        }

        @Test
        @DisplayName("Should open every hold in HELD")
        void shouldOpenHoldsAsHeld() {
            FlightInstance instance = flightInstance(FLIGHT_A);
            Reservation reservation = reservationWith(instance);

            seatReservationService.createHoldsForReservation(reservation, List.of(seat(1L, instance)));

            assertThat(capturedHolds())
                    .allSatisfy(hold -> assertThat(hold.getStatus()).isEqualTo(SeatReservationStatus.HELD));
        }

        @Test
        @DisplayName("Should expire every hold after the configured cart window")
        void shouldApplyConfiguredTtl() {
            FlightInstance instance = flightInstance(FLIGHT_A);
            Reservation reservation = reservationWith(instance);
            LocalDateTime before = LocalDateTime.now().plus(CART_TTL);

            seatReservationService.createHoldsForReservation(reservation, List.of(seat(1L, instance)));

            LocalDateTime after = LocalDateTime.now().plus(CART_TTL);

            assertThat(capturedHolds()).allSatisfy(hold ->
                    assertThat(hold.getHeldUntil()).isBetween(before, after));
        }

        @Test
        @DisplayName("Should give all holds of one booking the same expiry instant")
        void shouldShareOneExpiryAcrossTheBooking() {
            FlightInstance first = flightInstance(FLIGHT_A);
            FlightInstance second = flightInstance(FLIGHT_B);
            Reservation reservation = reservationWith(first, second);
            List<Seat> seats = List.of(seat(1L, first), seat(2L, second));

            seatReservationService.createHoldsForReservation(reservation, seats);

            List<LocalDateTime> expiries = capturedHolds().stream().map(SeatReservation::getHeldUntil).toList();

            assertThat(expiries).containsOnly(expiries.get(0));
        }
    }
}
