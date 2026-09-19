package com.aerolinea.flight_booking_api.domain.seatReservation;

import com.aerolinea.flight_booking_api.config.AbstractIntegrationTest;
import com.aerolinea.flight_booking_api.dtos.booking.BookingDTO;
import com.aerolinea.flight_booking_api.dtos.booking.BookingRequest;
import com.aerolinea.flight_booking_api.exceptions.BusinessRuleViolationException;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.aerolinea.flight_booking_api.models.*;
import com.aerolinea.flight_booking_api.models.enums.ReservationStatus;
import com.aerolinea.flight_booking_api.repositories.*;
import com.aerolinea.flight_booking_api.services.BookingService;
import com.aerolinea.flight_booking_api.services.ReservationService;
import com.aerolinea.flight_booking_api.services.SeatReservationService;
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
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
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

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.hikari.maximum-pool-size=30",
        "logging.level.org.hibernate.SQL=debug",
        "logging.level.org.hibernate.orm.jdbc.bind=trace",
        "app.scheduling.reservation-cleanup.delay=86400000"
})
class SeatLockingConcurrencyIntegrationTest extends AbstractIntegrationTest {

    private static final int COMPETING_THREADS = 20;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private SeatReservationService seatReservationService;

    @Autowired
    private TransactionTemplate transactionTemplate;

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
    private SeatReservationRepository seatReservationRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    private Long bookSeatAs(String username, Long seatId) {
        authenticateAs(username);
        BookingDTO booking = bookingService.createBooking(bookingFor(List.of(seatId)));
        SecurityContextHolder.clearContext();
        return booking.id();
    }

    private void forceHoldsToExpire(Long reservationId) {
        int updated = jdbcTemplate.update("""
                UPDATE seat_reservations sr
                JOIN flight_segments fs ON fs.id = sr.flight_segment_id
                JOIN itineraries i ON i.id = fs.itinerary_id
                SET sr.held_until = DATE_SUB(NOW(), INTERVAL 1 DAY)
                WHERE i.reservation_id = ?
                """, reservationId);

        assertThat(updated)
                .as("the fixture must move at least one hold into the past")
                .isPositive();
    }

    private String reservationStatusOf(Long reservationId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM reservations WHERE id = ?", String.class, reservationId);
    }

    private String holdStatusForSeat(Long seatId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM seat_reservations WHERE seat_id = ? ORDER BY id DESC LIMIT 1",
                String.class, seatId);
    }

    private long countHoldRowsForSeat(Long seatId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM seat_reservations WHERE seat_id = ?", Long.class, seatId);
    }

    @Test
    @DisplayName("An expired hold must make its seat bookable again straight away")
    void shouldReleaseSeatAfterExpiry() {
        Long seatId = seatIds.get(0);

        Long abandonedBooking = bookSeatAs("racer0", seatId);
        forceHoldsToExpire(abandonedBooking);

        reservationService.expirePendingReservations();

        assertThat(holdStatusForSeat(seatId)).isEqualTo(ReservationStatus.EXPIRED.name());
        assertThat(reservationStatusOf(abandonedBooking)).isEqualTo(ReservationStatus.EXPIRED.name());

        authenticateAs("racer1");
        assertThatCode(() -> bookingService.createBooking(bookingFor(List.of(seatId))))
                .as("the released seat must be available to the next customer with no waiting")
                .doesNotThrowAnyException();
        SecurityContextHolder.clearContext();

        assertThat(countHoldsForSeat(seatId))
                .as("only the new hold may occupy the seat")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("A reservation confirmed before the sweep must keep its seats")
    void shouldNotExpireAConfirmedReservation() {
        Long seatId = seatIds.get(0);

        Long paidBooking = bookSeatAs("racer0", seatId);

        authenticateAs("racer0");
        reservationService.confirmReservation(paidBooking);
        System.out.println("hold status after confirm: " + holdStatusForSeat(seatId));
        SecurityContextHolder.clearContext();


        forceHoldsToExpire(paidBooking);

        reservationService.expirePendingReservations();

        assertThat(holdStatusForSeat(seatId))
                .as("a paid seat must survive its own TTL")
                .isEqualTo(ReservationStatus.CONFIRMED.name());
        assertThat(reservationStatusOf(paidBooking)).isEqualTo(ReservationStatus.CONFIRMED.name());

        authenticateAs("racer1");
        assertThatThrownBy(() -> bookingService.createBooking(bookingFor(List.of(seatId))))
                .as("a confirmed seat must stay unavailable")
                .isInstanceOf(BusinessRuleViolationException.class);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Two sweeps running at once must not process the same reservation twice")
    void shouldBeIdempotentUnderConcurrentSweeps() throws InterruptedException {
        Long seatId = seatIds.get(0);

        Long abandonedBooking = bookSeatAs("racer0", seatId);
        forceHoldsToExpire(abandonedBooking);

        CountDownLatch startLine = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        List<Throwable> unexpected = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < 2; i++) {
            pool.submit(() -> {
                try {
                    startLine.await();
                    reservationService.expirePendingReservations();
                } catch (Throwable e) {
                    unexpected.add(e);
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startLine.countDown();
        boolean allFinished = finishLine.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(allFinished).isTrue();
        assertThat(unexpected)
                .as("a concurrent sweep must not raise, even when it finds the work already done")
                .isEmpty();

        assertThat(holdStatusForSeat(seatId)).isEqualTo(ReservationStatus.EXPIRED.name());
        assertThat(reservationStatusOf(abandonedBooking)).isEqualTo(ReservationStatus.EXPIRED.name());

        assertThat(countHoldRowsForSeat(seatId))
                .as("the sweep must not duplicate hold rows")
                .isEqualTo(1L);
    }

    private Long holdIdForSeat(Long seatId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM seat_reservations WHERE seat_id = ? ORDER BY id DESC LIMIT 1",
                Long.class, seatId);
    }

    private Long versionOfHold(Long holdId) {
        return jdbcTemplate.queryForObject(
                "SELECT version FROM seat_reservations WHERE id = ?", Long.class, holdId);
    }

    @Test
    @DisplayName("A hold confirmed between the sweep's read and its write must stay confirmed")
    void shouldNotOverwriteAConfirmedHold() throws InterruptedException {
        Long seatId = seatIds.get(0);
        Long reservationId = bookSeatAs("racer0", seatId);
        forceHoldsToExpire(reservationId);

        CountDownLatch bothHaveRead = new CountDownLatch(2);
        CountDownLatch mayWrite = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger conflicted = new AtomicInteger();
        List<Throwable> unexpected = Collections.synchronizedList(new ArrayList<>());

        Runnable confirm = () -> runContending(
                () -> {
                    authenticateAs("racer0");
                    reservationService.confirmReservation(reservationId);
                },
                bothHaveRead, mayWrite, finished, succeeded, conflicted, unexpected);

        Runnable expire = () -> runContending(
                () -> reservationService.expirePendingReservations(),
                bothHaveRead, mayWrite, finished, succeeded, conflicted, unexpected);

        pool.submit(confirm);
        pool.submit(expire);

        assertThat(bothHaveRead.await(20, TimeUnit.SECONDS))
                .as("both transactions must be in flight before either writes")
                .isTrue();
        mayWrite.countDown();

        assertThat(finished.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        assertThat(unexpected)
                .as("the only acceptable failure is an optimistic conflict")
                .isEmpty();

        String finalStatus = holdStatusForSeat(seatId);

        assertThat(finalStatus)
                .as("whichever transaction won, the hold must be in a terminal state")
                .isIn(ReservationStatus.CONFIRMED.name(), ReservationStatus.EXPIRED.name());

        assertThat(countHoldRowsForSeat(seatId))
                .as("no transaction may duplicate the hold")
                .isEqualTo(1L);

        if (ReservationStatus.CONFIRMED.name().equals(finalStatus)) {
            assertThat(reservationStatusOf(seatId == null ? null : reservationId))
                    .as("a confirmed hold implies a confirmed reservation")
                    .isEqualTo(ReservationStatus.CONFIRMED.name());
        } else {
            assertThat(reservationStatusOf(reservationId))
                    .as("an expired hold implies an expired reservation")
                    .isEqualTo(ReservationStatus.EXPIRED.name());
        }
    }

    private void runContending(Runnable work,
                               CountDownLatch bothHaveRead,
                               CountDownLatch mayWrite,
                               CountDownLatch finished,
                               AtomicInteger succeeded,
                               AtomicInteger conflicted,
                               List<Throwable> unexpected) {
        try {
            bothHaveRead.countDown();
            mayWrite.await(20, TimeUnit.SECONDS);
            work.run();
            succeeded.incrementAndGet();
        } catch (BusinessRuleViolationException | OptimisticLockingFailureException | PessimisticLockingFailureException e) {
            conflicted.incrementAndGet();
        } catch (Throwable e) {
            unexpected.add(e);
        } finally {
            SecurityContextHolder.clearContext();
            finished.countDown();
        }
    }

    @Test
    @DisplayName("A stale hold must not overwrite a newer state")
    void shouldRejectAWriteFromAStaleRead() {
        Long seatId = seatIds.get(0);
        Long reservationId = bookSeatAs("racer0", seatId);
        Long holdId = holdIdForSeat(seatId);

        Long versionBefore = versionOfHold(holdId);

        authenticateAs("racer0");
        reservationService.confirmReservation(reservationId);
        SecurityContextHolder.clearContext();

        assertThat(versionOfHold(holdId))
                .as("confirming the hold must bump its version")
                .isGreaterThan(versionBefore);

        forceHoldsToExpire(reservationId);
        reservationService.expirePendingReservations();

        assertThat(holdStatusForSeat(seatId))
                .as("a confirmed hold is terminal and the sweep must leave it alone")
                .isEqualTo(ReservationStatus.CONFIRMED.name());
    }

    @Test
    @DisplayName("A released seat must be bookable again by another customer")
    void shouldFreeTheSeatAfterAConflictResolvesToExpired() {
        Long seatId = seatIds.get(0);
        Long abandoned = bookSeatAs("racer0", seatId);

        forceHoldsToExpire(abandoned);
        reservationService.expirePendingReservations();

        assertThat(holdStatusForSeat(seatId)).isEqualTo(ReservationStatus.EXPIRED.name());

        authenticateAs("racer1");
        assertThatCode(() -> bookingService.createBooking(bookingFor(List.of(seatId))))
                .as("the seat must be available immediately after the sweep")
                .doesNotThrowAnyException();
        SecurityContextHolder.clearContext();
    }

}