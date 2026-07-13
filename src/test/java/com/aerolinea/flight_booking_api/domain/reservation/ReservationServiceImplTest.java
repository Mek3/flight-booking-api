package com.aerolinea.flight_booking_api.domain.reservation;

import com.aerolinea.flight_booking_api.exceptions.BusinessRuleViolationException;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.aerolinea.flight_booking_api.exceptions.ResourceNotFoundException;
import com.aerolinea.flight_booking_api.models.Flight;
import com.aerolinea.flight_booking_api.models.Reservation;
import com.aerolinea.flight_booking_api.models.ReservationStatus;
import com.aerolinea.flight_booking_api.models.User;
import com.aerolinea.flight_booking_api.repositories.ReservationRepository;
import com.aerolinea.flight_booking_api.services.ReservationServiceImpl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private User testUser;
    private Flight testFlight;
    private Reservation testReservation;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("pacog")
                .email("pacog@example.com")
                .build();

        ReflectionTestUtils.setField(testUser, "id", 1L);

        testFlight = Flight.builder()
                .flightNumber("IBE-001")
                .departure("MAD")
                .departureTime(LocalDateTime.now().plusDays(5))
                .destination("JFK")
                .destinationTime(LocalDateTime.now().plusDays(5))
                .availableSeats(100)
                .price(new BigDecimal("400.00"))
                .build();

        testReservation = Reservation.builder()
                .reservationCode("RES-12345")
                .status(ReservationStatus.PENDING)
                .numberOfPassengers(2)
                .totalPrice(new BigDecimal("800.00"))
                .user(testUser)
                .flight(testFlight)
                .build();

    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext(String username, String role) {
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn(username);
        
        GrantedAuthority authority = new SimpleGrantedAuthority(role);
        lenient().doReturn(List.of(authority)).when(authentication).getAuthorities();
    }

    @Test
    @DisplayName("Should confirm reservation successfully if it belongs to the user")
    void shouldConfirmReservationSuccessfully() {
        mockSecurityContext("pacog", "ROLE_USER");
        when(reservationRepository.findByIdAndUserUsername(100L, "pacog"))
                .thenReturn(Optional.of(testReservation));

        reservationService.confirmReservation(100L);

        assertEquals(ReservationStatus.CONFIRMED, testReservation.getStatus());
        verify(reservationRepository, times(1)).save(testReservation);
    }

    @Test
    @DisplayName("Should throw exception when confirming if reservation does not exist or does not belong to the user")
    void shouldThrowExceptionWhenConfirmingNonExistentReservation() {
        mockSecurityContext("hacker", "ROLE_USER");
        when(reservationRepository.findByIdAndUserUsername(100L, "hacker"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> reservationService.confirmReservation(100L));
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Owner should be able to cancel reservation with more than 24h notice")
    void ownerShouldCancelReservationSuccessfully() {
        mockSecurityContext("pacog", "ROLE_USER");
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(testReservation));
        int initialSeats = testFlight.getAvailableSeats();

        reservationService.cancelReservation(100L);

        assertEquals(ReservationStatus.CANCELLED, testReservation.getStatus());
        assertEquals(initialSeats + 2, testFlight.getAvailableSeats()); 
    }

    @Test
    @DisplayName("Admin should be able to cancel another user's reservation")
    void adminShouldCancelAnyReservationSuccessfully() {
        mockSecurityContext("admin_system", "ROLE_ADMIN");
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(testReservation));

        reservationService.cancelReservation(100L);

        assertEquals(ReservationStatus.CANCELLED, testReservation.getStatus());
    }

    @Test
    @DisplayName("Should throw exception if a user tries to cancel another user's reservation")
    void shouldThrowExceptionWhenCancellingSomeoneElseReservation() {
        mockSecurityContext("thief", "ROLE_USER");
        Long idReservation = 100L;
        when(reservationRepository.findById(idReservation)).thenReturn(Optional.of(testReservation));

        BusinessRuleViolationException exception = assertThrows(
                BusinessRuleViolationException.class, 
                () -> reservationService.cancelReservation(idReservation)
        );
        assertTrue(exception.getMessage().contains(String.format(ErrorCode.INSUFFICIENT_PERMISSIONS.getMessage(), "thief")));
    }

    @Test
    @DisplayName("Should throw exception if attempting to cancel with less than 24 hours notice")
    void shouldThrowExceptionWhenCancellingTooLate() {
        mockSecurityContext("pacog", "ROLE_USER");
        Long idReservation = 100L;

        ReflectionTestUtils.setField(testFlight, "departureTime", LocalDateTime.now().plusHours(10));
        
        when(reservationRepository.findById(idReservation)).thenReturn(Optional.of(testReservation));

        BusinessRuleViolationException exception = assertThrows(
                BusinessRuleViolationException.class, 
                () -> reservationService.cancelReservation(idReservation)
        );
        assertTrue(exception.getMessage().contains(String.format(ErrorCode.CANCELLATION_TIME_EXPIRED.getMessage(), idReservation)));
    }
}