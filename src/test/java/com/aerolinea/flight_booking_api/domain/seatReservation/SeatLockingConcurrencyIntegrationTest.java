package com.aerolinea.flight_booking_api.domain.seatReservation;

import com.aerolinea.flight_booking_api.config.AbstractIntegrationTest;
import com.aerolinea.flight_booking_api.dtos.booking.BookingRequest;
import com.aerolinea.flight_booking_api.exceptions.BusinessRuleViolationException;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.aerolinea.flight_booking_api.models.*;
import com.aerolinea.flight_booking_api.repositories.*;
import com.aerolinea.flight_booking_api.services.BookingService;
import com.aerolinea.flight_booking_api.utils.factories.AircraftLayoutFactory;
import com.aerolinea.flight_booking_api.utils.factories.AircraftModelFactory;
import com.aerolinea.flight_booking_api.utils.factories.AirportFactory;
import com.aerolinea.flight_booking_api.utils.factories.FlightInstanceFactory;
import com.aerolinea.flight_booking_api.utils.factories.FlightScheduleFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.hikari.maximum-pool-size=30",
        "spring.jpa.show-sql=false"
})
class SeatLockingConcurrencyIntegrationTest extends AbstractIntegrationTest {

    private static final int COMPETING_THREADS = 20;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private AircraftModelRepository aircraftModelRepository;

    @Autowired
    private AircraftLayoutRepository aircraftLayoutRepository;

    @Autowired
    private FlightScheduleRepository flightScheduleRepository;

    @Autowired
    private FlightInstanceRepository flightInstanceRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private FlightInstance flightInstance;
    private List<Long> seatIds;

    @BeforeEach
    void setUp() {
        Airport departure = airportRepository.save(AirportFactory.validAirportBuilder("MAD").build());
        Airport arrival = airportRepository.save(AirportFactory.validAirportBuilder("JFK").build());
        AircraftModel model = aircraftModelRepository.save(AircraftModelFactory.validModelBuilder().build());
        AircraftLayout layout = aircraftLayoutRepository.save(AircraftLayoutFactory.validLayoutBuilder(model).build());

        FlightSchedule schedule = flightScheduleRepository.save(
                FlightScheduleFactory.validScheduleBuilder(departure, arrival, layout)
                        .flightNumber("LOCK-001")
                        .build());

        flightInstance = flightInstanceRepository.save(
                FlightInstanceFactory.validInstanceBuilder(schedule).build());

        seatRepository.batchInsertSeats(flightInstance.getId(), 2, "AB");

        seatIds = seatRepository.findAll().stream()
                .filter(seat -> seat.getFlightInstance().getId().equals(flightInstance.getId()))
                .map(Seat::getId)
                .sorted()
                .toList();

        for (int i = 0; i < COMPETING_THREADS; i++) {
            userRepository.save(User.builder()
                    .username("racer" + i)
                    .email("racer" + i + "@test.com")
                    .password("encoded")
                    .name("Racer")
                    .surname(String.valueOf(i))
                    .build());
        }
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM seat_reservations");
        jdbcTemplate.execute("DELETE FROM flight_segments");
        jdbcTemplate.execute("DELETE FROM itineraries");
        jdbcTemplate.execute("DELETE FROM reservations");
        jdbcTemplate.execute("DELETE FROM seats");
        jdbcTemplate.execute("DELETE FROM flight_instances");
        jdbcTemplate.execute("DELETE FROM flight_schedules");
        jdbcTemplate.execute("DELETE FROM aircraft_layouts");
        jdbcTemplate.execute("DELETE FROM aircraft_models");
        jdbcTemplate.execute("DELETE FROM airports");
        jdbcTemplate.execute("DELETE FROM users_roles");
        jdbcTemplate.execute("DELETE FROM users");
        SecurityContextHolder.clearContext();
    }

    private BookingRequest bookingFor(List<Long> requestedSeatIds) {
        return new BookingRequest(requestedSeatIds.size(),
                List.of(new BookingRequest.ItineraryRequest(
                        List.of(new BookingRequest.FlightSegmentRequest(flightInstance.getId(), requestedSeatIds)))));
    }

    private void authenticateAs(String username) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(username, null, List.of()));
        SecurityContextHolder.setContext(context);
    }

    private long countHoldsForSeat(Long seatId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM seat_reservations WHERE seat_id = ? AND occupied_flag IS NOT NULL",
                Long.class, seatId);
    }

    @Test
    @DisplayName("Twenty threads competing for the same seat must produce exactly one winner")
    void shouldAllowOnlyOneWinnerForTheSameSeat() throws InterruptedException {
        Long contestedSeat = seatIds.get(0);

        CountDownLatch startLine = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(COMPETING_THREADS);
        ExecutorService pool = Executors.newFixedThreadPool(COMPETING_THREADS);

        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Throwable> unexpected = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < COMPETING_THREADS; i++) {
            final String username = "racer" + i;

            pool.submit(() -> {
                try {
                    startLine.await();
                    authenticateAs(username);
                    bookingService.createBooking(bookingFor(List.of(contestedSeat)));
                    succeeded.incrementAndGet();
                } catch (BusinessRuleViolationException e) {
                    rejected.incrementAndGet();
                } catch (Throwable e) {
                    unexpected.add(e);
                } finally {
                    SecurityContextHolder.clearContext();
                    finishLine.countDown();
                }
            });
        }

        startLine.countDown();
        boolean allFinished = finishLine.await(60, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(allFinished).as("every thread must finish before the timeout").isTrue();
        assertThat(unexpected)
                .as("failures must be business rejections, not infrastructure errors")
                .isEmpty();
        assertThat(succeeded.get()).as("exactly one booking may win the seat").isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(COMPETING_THREADS - 1);
        assertThat(countHoldsForSeat(contestedSeat)).as("the seat must never be held twice").isEqualTo(1);
    }

    @Test
    @DisplayName("Two transactions requesting the same seats in reverse order must not deadlock")
    void shouldNotDeadlockOnReverseOrderRequests() throws InterruptedException {
        Long firstSeat = seatIds.get(0);
        Long secondSeat = seatIds.get(1);

        CountDownLatch startLine = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Throwable> lockFailures = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> unexpected = Collections.synchronizedList(new ArrayList<>());

        List<List<Long>> requestedOrders = List.of(
                List.of(firstSeat, secondSeat),
                List.of(secondSeat, firstSeat));

        for (int i = 0; i < 2; i++) {
            final String username = "racer" + i;
            final List<Long> order = requestedOrders.get(i);

            pool.submit(() -> {
                try {
                    startLine.await();
                    authenticateAs(username);
                    bookingService.createBooking(bookingFor(order));
                    succeeded.incrementAndGet();
                } catch (BusinessRuleViolationException e) {
                    rejected.incrementAndGet();
                } catch (DeadlockLoserDataAccessException | CannotAcquireLockException e) {
                    lockFailures.add(e);
                } catch (Throwable e) {
                    unexpected.add(e);
                } finally {
                    SecurityContextHolder.clearContext();
                    finishLine.countDown();
                }
            });
        }

        startLine.countDown();
        boolean allFinished = finishLine.await(60, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(allFinished).as("neither transaction may hang").isTrue();
        assertThat(lockFailures)
                .as("sorting seat ids before locking removes the deadlock cycle")
                .isEmpty();
        assertThat(unexpected).isEmpty();
        assertThat(succeeded.get()).as("one booking wins the contested pair").isEqualTo(1);
        assertThat(rejected.get()).as("the other is rejected cleanly, not by a lock error").isEqualTo(1);
        assertThat(countHoldsForSeat(firstSeat)).isEqualTo(1);
        assertThat(countHoldsForSeat(secondSeat)).isEqualTo(1);
    }

    @Test
    @DisplayName("A booking must hold every requested seat or none at all")
    void shouldHoldAllSeatsOrNone() throws InterruptedException {
        Long freeSeat = seatIds.get(0);
        Long takenSeat = seatIds.get(1);

        authenticateAs("racer0");
        bookingService.createBooking(bookingFor(List.of(takenSeat)));
        SecurityContextHolder.clearContext();

        authenticateAs("racer1");

        try {
            bookingService.createBooking(bookingFor(List.of(freeSeat, takenSeat)));
        } catch (BusinessRuleViolationException expected) {
            // the second seat is unavailable, so the whole booking must be abandoned
        }

        SecurityContextHolder.clearContext();

        assertThat(countHoldsForSeat(freeSeat))
                .as("the seat that was available must not stay held after the booking failed")
                .isZero();
        assertThat(countHoldsForSeat(takenSeat)).isEqualTo(1);
    }


    @Test
    @DisplayName("A booking must give up rather than wait indefinitely for a locked seat")
    void shouldNotWaitIndefinitelyForALockedSeat() throws InterruptedException {
        Long contestedSeat = seatIds.get(0);

        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);
        ExecutorService lockHolder = Executors.newSingleThreadExecutor();

        lockHolder.submit(() -> transactionTemplate.execute(status -> {
            seatRepository.lockSeats(List.of(contestedSeat));
            lockAcquired.countDown();

            try {
                releaseLock.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return null;
        }));

        assertThat(lockAcquired.await(10, TimeUnit.SECONDS))
                .as("the holder thread must take the lock before the booking starts")
                .isTrue();

        authenticateAs("racer0");

        long startedAt = System.nanoTime();
        Throwable thrown = catchThrowable(() -> bookingService.createBooking(bookingFor(List.of(contestedSeat))));
        Duration waited = Duration.ofNanos(System.nanoTime() - startedAt);

        releaseLock.countDown();
        lockHolder.shutdown();
        SecurityContextHolder.clearContext();

        assertThat(thrown)
                .as("a contended seat must be reported as a retryable conflict")
                .isInstanceOf(BusinessRuleViolationException.class);

        assertThat(((BusinessRuleViolationException) thrown).getErrorCode())
                .isEqualTo(ErrorCode.SEAT_CURRENTLY_LOCKED);

        assertThat(waited)
                .as("the request must fail fast instead of blocking the thread")
                .isLessThan(Duration.ofSeconds(10));

        assertThat(countHoldsForSeat(contestedSeat))
                .as("a rejected booking must leave no hold behind")
                .isZero();
    }
}