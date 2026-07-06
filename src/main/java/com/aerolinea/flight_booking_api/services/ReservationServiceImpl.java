package com.aerolinea.flight_booking_api.services;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.aerolinea.flight_booking_api.dtos.ReservationDTO;
import com.aerolinea.flight_booking_api.dtos.ReservationRequest;
import com.aerolinea.flight_booking_api.exceptions.BusinessRuleViolationException;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.aerolinea.flight_booking_api.exceptions.ResourceNotFoundException;
import com.aerolinea.flight_booking_api.mappers.ReservationMapper;
import com.aerolinea.flight_booking_api.models.Flight;
import com.aerolinea.flight_booking_api.models.Reservation;
import com.aerolinea.flight_booking_api.models.ReservationStatus;
import com.aerolinea.flight_booking_api.models.User;
import com.aerolinea.flight_booking_api.repositories.FlightRepository;
import com.aerolinea.flight_booking_api.repositories.ReservationRepository;
import com.aerolinea.flight_booking_api.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationServiceImpl implements ReservationService {

    private final UserRepository userRepository;
    private final FlightRepository flightRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationMapper reservationMapper;

    @Autowired
    @Lazy
    private ReservationService reservationService;

    private boolean isAdmin() {
        return getAuthenticator().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    private Authentication getAuthenticator() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Override
    @Transactional
    public ReservationDTO createReservation(ReservationRequest reservationRequest) {

        String username = getAuthenticator().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND,
                        String.format(ErrorCode.USER_NOT_FOUND.getMessage(), username)));

        Flight flight = flightRepository.findById(reservationRequest.flightId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.FLIGHT_NOT_FOUND,
                        String.format(ErrorCode.FLIGHT_NOT_FOUND.getMessage(), reservationRequest.flightId())));

        if (flight.getAvailableSeats() < reservationRequest.numberOfPassengers()) {
            throw new BusinessRuleViolationException(ErrorCode.NOT_ENOUGH_SEATS,
                    String.format(ErrorCode.NOT_ENOUGH_SEATS.getMessage(), reservationRequest.flightId()));
        }

        Reservation reservation = Reservation.builder()
                .reservationCode(UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status(ReservationStatus.PENDING)
                .numberOfPassengers(reservationRequest.numberOfPassengers())
                .totalPrice(flight.getPrice().multiply(BigDecimal.valueOf(reservationRequest.numberOfPassengers())))
                .user(user)
                .flight(flight)
                .build();

        flightRepository.save(flight);

        Reservation savedReservation = reservationRepository.saveAndFlush(reservation);

        log.info("Reservation successfully created. Code: {} | User: {} | Flight ID: {}",
                savedReservation.getReservationCode(), username, flight.getId());

        return reservationMapper.toReservationDTO(savedReservation);
    }

    @Override
    @Transactional
    public void cancelReservation(Long idReservation) {

        String username = getAuthenticator().getName();

        if (username == null) {
            throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND,
                    String.format(ErrorCode.USER_NOT_FOUND.getMessage(), "unknown"));
        }

        Reservation reservation = reservationRepository.findById(idReservation)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESERVATION_NOT_FOUND,
                        String.format(ErrorCode.RESERVATION_NOT_FOUND.getMessage(), idReservation)));

        boolean isOwner = reservation.getUser() != null && reservation.getUser().getUsername().equals(username);

        if (!isAdmin() && !isOwner) {
            throw new BusinessRuleViolationException(ErrorCode.INSUFFICIENT_PERMISSIONS,
                    String.format(ErrorCode.INSUFFICIENT_PERMISSIONS.getMessage(), username));
        }

        if (reservation.getFlight() == null) {
            throw new ResourceNotFoundException(ErrorCode.FLIGHT_NOT_FOUND,
                    String.format(ErrorCode.FLIGHT_NOT_FOUND.getMessage(), "unknown"));
        }

        Duration diff = Duration.between(LocalDateTime.now(), reservation.getFlight().getDepartureTime());
        if (diff.toHours() <= 24) {
            throw new BusinessRuleViolationException(ErrorCode.CANCELLATION_TIME_EXPIRED,
                    String.format(ErrorCode.CANCELLATION_TIME_EXPIRED.getMessage(), idReservation));
        }

        reservation.cancelReservation();
        reservation.getFlight().increaseAvailableSeats(reservation.getNumberOfPassengers());
    }

    @Override
    public ReservationDTO getReservationByIdAndUsername(Long idReservation) {
        String username = getAuthenticator().getName();
        return reservationMapper.toReservationDTO(
                reservationRepository.findByIdAndUserUsername(idReservation, username)
                        .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESERVATION_NOT_FOUND,
                                String.format(ErrorCode.RESERVATION_NOT_FOUND.getMessage(), idReservation))));
    }

    @Override
    public ReservationDTO getReservationById(Long idReservation) {
        return reservationMapper.toReservationDTO(
                reservationRepository.findById(idReservation)
                        .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESERVATION_NOT_FOUND,
                                String.format(ErrorCode.RESERVATION_NOT_FOUND.getMessage(), idReservation))));
    }

    @Override
    public Page<ReservationDTO> getReservationsByUsername(Pageable pageable) {
        return reservationRepository.findByUserUsername(pageable, getAuthenticator().getName())
                .map(reservationMapper::toReservationDTO);
    }

    @Override
    public Page<ReservationDTO> getReservations(Pageable pageable) {
        return reservationRepository.findAll(pageable).map(reservationMapper::toReservationDTO);
    }
  
    @Override
    public void confirmReservation(Long id) {
       String username = getAuthenticator().getName();
       Reservation reservation = reservationRepository.findByIdAndUserUsername(id, username)
                            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESERVATION_NOT_FOUND, 
                                                String.format(ErrorCode.RESERVATION_NOT_FOUND.getMessage(), id)));
        reservation.confirmReservation();
        reservationRepository.save(reservation);
        log.info("Reservation successfully confirmed ID: {}", id);
    }

    @Override
    public void expirePendingReservations() {
        
        List<Long> expiredReservationIds = reservationRepository.findExpiredReservationIds(
                ReservationStatus.PENDING, LocalDateTime.now().minusHours(15));

        if (expiredReservationIds.isEmpty()) {
            return;
        }

        log.info("Processing expiration for {} pending reservations.", expiredReservationIds.size());

        int successCount = 0;
        for (Long idReservation : expiredReservationIds) {
            try {
                reservationService.processSingleExpiration(idReservation);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to expire reservation ID: {}. Reason: {}", idReservation, e.getMessage());
            }
        }
        
        log.info("Successfully expired {}/{} reservations.", successCount, expiredReservationIds.size());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleExpiration(Long idReservation) {

        Reservation reservation = reservationRepository.findByIdWithFlight(idReservation)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESERVATION_NOT_FOUND, 
                                                    String.format(ErrorCode.RESERVATION_NOT_FOUND.getMessage(), idReservation)));

        Flight flight = reservation.getFlight();

        if (flight == null) {
            log.error("Data Integrity Violation: Reservation ID {} lacks a valid Flight. Skipping.", reservation.getId());
            return;
        }

        flight.increaseAvailableSeats(reservation.getNumberOfPassengers());
        
        reservation.expireReservation();

        log.debug("Reservation {} expired. Seats restored to flight ID: {}", 
                reservation.getReservationCode(), flight.getId());
    } 

}