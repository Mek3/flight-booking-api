package com.aerolinea.flight_booking_api.domain.reservation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.aerolinea.flight_booking_api.config.AbstractIntegrationTest;
import com.aerolinea.flight_booking_api.dtos.ReservationRequest;
import com.aerolinea.flight_booking_api.models.Flight;
import com.aerolinea.flight_booking_api.models.User;
import com.aerolinea.flight_booking_api.repositories.FlightRepository;
import com.aerolinea.flight_booking_api.repositories.UserRepository;
import com.aerolinea.flight_booking_api.services.ReservationService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FlightConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long targetFlightId;
    private final String TEST_USERNAME = "concurrency_user";

    @BeforeEach
    void setUp() {
        reservationService.getClass();
        
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("SYSTEM_TEST", null, Collections.emptyList()));
        SecurityContextHolder.setContext(context);

        try {
            User testUser = User.builder()
                    .username(TEST_USERNAME)
                    .email("concurrency@test.com")
                    .password("encoded_password_here")
                    .name("TestName")
                    .surname("TestSurname")
                    .phone("123456789")
                    .build();
            userRepository.saveAndFlush(testUser);
            
            Flight flight = Flight.builder()
                    .flightNumber("RACE-101")
                    .departure("MAD")
                    .departureTime(LocalDateTime.now().plusDays(1))
                    .destination("JFK")
                    .destinationTime(LocalDateTime.now().plusDays(1).plusHours(8))
                    .availableSeats(1)
                    .price(new BigDecimal("450.50"))
                    .build();
            
            targetFlightId = flightRepository.saveAndFlush(flight).getId();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Autowired
    void tearDown() {
       if (targetFlightId != null) {
            jdbcTemplate.update("DELETE FROM reservations WHERE flight_id = ?", targetFlightId);
            
            jdbcTemplate.update("DELETE FROM flights WHERE id = ?", targetFlightId);
        }

       jdbcTemplate.update("DELETE FROM users WHERE username = ?", TEST_USERNAME);
    }


    @Test
    void givenOneAvailableSeat_whenFiftyConcurrentBookingAttempts_thenOnlyOneSucceeds() throws InterruptedException {
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successfulReservations = new AtomicInteger(0);
        AtomicInteger failedReservations = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(new UsernamePasswordAuthenticationToken(TEST_USERNAME, null, Collections.emptyList()));
                    SecurityContextHolder.setContext(context);

                    startLatch.await();
                    
                    ReservationRequest reservationRequest = new ReservationRequest(targetFlightId, 1);
                    reservationService.createReservation(reservationRequest); 
                    successfulReservations.incrementAndGet();
                    
                } catch (ObjectOptimisticLockingFailureException | org.springframework.dao.CannotAcquireLockException e) {
                    failedReservations.incrementAndGet();
                } catch (com.aerolinea.flight_booking_api.exceptions.BusinessRuleViolationException e) {
                    failedReservations.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Unexpected error in thread: " + e.getClass().getName() + " - " + e.getMessage());
                } finally {
                    SecurityContextHolder.clearContext();
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); 
        doneLatch.await();      
        executor.shutdown();

        assertThat(successfulReservations.get())
                .as("Exactly one transaction should succeed")
                .isEqualTo(1);
                
        assertThat(failedReservations.get())
                .as("The remaining 49 transactions must fail and be caught by the defense layers")
                .isEqualTo(threadCount - 1);

        Flight updatedFlight = flightRepository.findById(targetFlightId).orElseThrow();
        assertThat(updatedFlight.getAvailableSeats())
                .as("The flight must have exactly 0 seats available, guaranteeing NO overbooking")
                .isEqualTo(0);
        
        assertThat(updatedFlight.getVersion())
                .as("The entity version must have incremented by exactly 1 due to the single successful transaction")
                .isEqualTo(1L); 
    }
}