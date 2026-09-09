package com.aerolinea.flight_booking_api.domain.reservation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

@SpringBootTest
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

    @AfterEach
    void tearDown() {
       if (targetFlightId != null) {
            jdbcTemplate.update("DELETE FROM reservations WHERE flight_id = ?", targetFlightId);
            
            jdbcTemplate.update("DELETE FROM flights WHERE id = ?", targetFlightId);
        }

       jdbcTemplate.update("DELETE FROM users WHERE username = ?", TEST_USERNAME);
    }



}