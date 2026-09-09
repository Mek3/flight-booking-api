package com.aerolinea.flight_booking_api.domain.booking;

import com.aerolinea.flight_booking_api.dtos.booking.BookingRequest;
import com.aerolinea.flight_booking_api.exceptions.BusinessRuleViolationException;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.aerolinea.flight_booking_api.exceptions.ResourceNotFoundException;
import com.aerolinea.flight_booking_api.mappers.BookingMapper;
import com.aerolinea.flight_booking_api.mappers.ReservationMapper;
import com.aerolinea.flight_booking_api.models.*;
import com.aerolinea.flight_booking_api.models.enums.FlightStatus;
import com.aerolinea.flight_booking_api.repositories.FlightInstanceRepository;
import com.aerolinea.flight_booking_api.repositories.ReservationRepository;
import com.aerolinea.flight_booking_api.repositories.UserRepository;
import com.aerolinea.flight_booking_api.services.BookingFactory;
import com.aerolinea.flight_booking_api.services.BookingServiceImpl;
import com.aerolinea.flight_booking_api.services.ReservationServiceImpl;

import com.aerolinea.flight_booking_api.services.routing.ItineraryRoutingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingCreationTest {

    private static final String USERNAME = "pacog";
    private static final LocalDate OPERATING_DATE = LocalDate.of(2026, 10, 15);

    @Mock
    private UserRepository userRepository;

    @Mock
    private FlightInstanceRepository flightInstanceRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ItineraryRoutingService itineraryRoutingService;

    @Mock
    private ReservationMapper reservationMapper;

    @Spy
    private BookingFactory bookingFactory = new BookingFactory();

    @InjectMocks
    private BookingServiceImpl bookingService;
    @Mock
    private BookingMapper bookingMapper;

    private User testUser;

    @BeforeEach
    void setUp() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(USERNAME, null, List.of()));
        SecurityContextHolder.setContext(context);

        testUser = User.builder().username(USERNAME).email("pacog@example.com").build();
        ReflectionTestUtils.setField(testUser, "id", 1L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private FlightInstance instance(Long id, String from, String to, int departureHour, int arrivalHour, String fare) {
        FlightSchedule schedule = FlightSchedule.builder()
                .flightNumber("IBE-" + id)
                .departureAirport(Airport.builder().code(from).name(from).city(from).country("ES").build())
                .arrivalAirport(Airport.builder().code(to).name(to).city(to).country("ES").build())
                .departureTime(LocalTime.of(departureHour, 0))
                .arrivalTime(LocalTime.of(arrivalHour, 0))
                .arrivalDayOffset(0)
                .daysOfWeekMask(127)
                .basePrice(new BigDecimal(fare))
                .build();

        FlightInstance flightInstance = FlightInstance.builder()
                .flightSchedule(schedule)
                .departureDate(OPERATING_DATE)
                .status(FlightStatus.SCHEDULED)
                .build();

        ReflectionTestUtils.setField(flightInstance, "id", id);
        return flightInstance;
    }

    private BookingRequest request(int passengers, List<Long> instanceIds) {
        return new BookingRequest(passengers, List.of(new BookingRequest.ItineraryRequest(instanceIds)));
    }

    private void givenUserExists() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(testUser));
    }

    private void givenInstances(FlightInstance... instances) {
        when(flightInstanceRepository.findByIdInWithSchedule(anyList())).thenReturn(List.of(instances));
    }

    @Nested
    @DisplayName("Successful creation")
    class HappyPath {

        @Test
        @DisplayName("Should link segments in the order they were requested")
        void shouldPreserveSegmentOrder() {
            givenUserExists();
            givenInstances(
                    instance(101L, "ALC", "MAD", 8, 9, "100.00"),
                    instance(205L, "MAD", "JFK", 12, 20, "400.00"));
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(call -> call.getArgument(0));

            bookingService.createBooking(request(1, List.of(101L, 205L)));

            ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
            verify(reservationRepository).save(captor.capture());

            List<FlightSegment> segments = captor.getValue().getItineraries().get(0).getSegments();

            assertThat(segments).hasSize(2);
            assertThat(segments.get(0).getSegmentOrder()).isEqualTo(1);
            assertThat(segments.get(0).getFlightInstance().getId()).isEqualTo(101L);
            assertThat(segments.get(1).getSegmentOrder()).isEqualTo(2);
            assertThat(segments.get(1).getFlightInstance().getId()).isEqualTo(205L);
        }

        @Test
        @DisplayName("Should sum segment fares and scale by passenger count")
        void shouldCalculateTotalPrice() {
            givenUserExists();
            givenInstances(
                    instance(101L, "ALC", "MAD", 8, 9, "100.00"),
                    instance(205L, "MAD", "JFK", 12, 20, "400.00"));
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(call -> call.getArgument(0));

            bookingService.createBooking(request(3, List.of(101L, 205L)));

            ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
            verify(reservationRepository).save(captor.capture());

            assertThat(captor.getValue().getTotalPrice()).isEqualByComparingTo(new BigDecimal("1500.00"));
        }

        @Test
        @DisplayName("Should open a booking in PENDING with a generated reservation code")
        void shouldStartPending() {
            givenUserExists();
            givenInstances(instance(101L, "ALC", "MAD", 8, 9, "100.00"));
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(call -> call.getArgument(0));

            bookingService.createBooking(request(1, List.of(101L)));

            ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
            verify(reservationRepository).save(captor.capture());

            assertThat(captor.getValue().getStatus()).isEqualTo(ReservationStatus.PENDING);
            assertThat(captor.getValue().getReservationCode()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Failure before persistence")
    class FailFast {

        @Test
        @DisplayName("Should not persist anything when routing validation rejects the itinerary")
        void shouldAbortOnInvalidItinerary() {
            givenUserExists();
            givenInstances(
                    instance(101L, "ALC", "MAD", 8, 9, "100.00"),
                    instance(205L, "BCN", "JFK", 12, 20, "400.00"));

            doThrow(new BusinessRuleViolationException(ErrorCode.SEGMENT_AIRPORT_DISCONTINUITY, "discontinuity"))
                    .when(itineraryRoutingService).validate(any(Itinerary.class));

            assertThatThrownBy(() -> bookingService.createBooking(request(1, List.of(101L, 205L))))
                    .isInstanceOf(BusinessRuleViolationException.class);

            verify(reservationRepository, never()).save(any(Reservation.class));
        }

        @Test
        @DisplayName("Should reject a request naming an unknown flight instance")
        void shouldRejectUnknownInstance() {
            givenUserExists();
            when(flightInstanceRepository.findByIdInWithSchedule(anyList())).thenReturn(List.of());

            assertThatThrownBy(() -> bookingService.createBooking(request(1, List.of(999L))))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(reservationRepository, never()).save(any(Reservation.class));
            verify(itineraryRoutingService, never()).validate(any(Itinerary.class));
        }

        @Test
        @DisplayName("Should reject a booking for an unknown user")
        void shouldRejectUnknownUser() {
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.createBooking(request(1, List.of(101L))))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(reservationRepository, never()).save(any(Reservation.class));
        }
    }
}