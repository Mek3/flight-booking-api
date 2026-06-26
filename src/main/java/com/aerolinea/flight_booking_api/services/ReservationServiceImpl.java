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

        Reservation reservation = new Reservation();

        String username = getAuthenticator().getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND,
                        String.format(ErrorCode.USER_NOT_FOUND.getMessage(), username)));

        reservation.setUser(user);

        Flight flight = flightRepository.findById(reservationRequest.flightId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.FLIGHT_NOT_FOUND,
                        String.format(ErrorCode.FLIGHT_NOT_FOUND.getMessage(), reservationRequest.flightId())));

        if (flight.getAvailableSeats() < reservationRequest.numberOfPassengers()) {
            throw new BusinessRuleViolationException(ErrorCode.NOT_ENOUGH_SEATS,
                    String.format(ErrorCode.NOT_ENOUGH_SEATS.getMessage(), reservationRequest.flightId()));
        }

        flight.setAvailableSeats(flight.getAvailableSeats() - reservationRequest.numberOfPassengers());
        reservation.setFlight(flight);

        BigDecimal totalPrice = flight.getPrice().multiply(BigDecimal.valueOf(reservationRequest.numberOfPassengers()));
        reservation.setTotalPrice(totalPrice);

        reservation.setNumberOfPassengers(reservationRequest.numberOfPassengers());
        reservation.setStatus(ReservationStatus.PENDING);

        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        reservation.setReservationCode(uuid);

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

        if (reservation.getStatus().equals(ReservationStatus.CANCELLED)) {
            throw new BusinessRuleViolationException(ErrorCode.RESERVATION_ALREADY_CANCELLED,
                    String.format(ErrorCode.RESERVATION_ALREADY_CANCELLED.getMessage(), idReservation));
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

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.getFlight().setAvailableSeats(reservation.getFlight().getAvailableSeats() + reservation.getNumberOfPassengers());
        reservationRepository.save(reservation);
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
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);
        log.info("Reservation successfully confirmed ID: {}", id);
    }


    @Override
    public void expirePendingReservations() {
        
        List<Reservation> expiredReservations = reservationRepository
                .findByStatusAndCreatedAtBefore(ReservationStatus.PENDING, LocalDateTime.now().minusMinutes(15));

        if (expiredReservations.isEmpty()) {
            return;
        }

        log.info("Processing expiration for {} pending reservations.", expiredReservations.size());

        int successCount = 0;
        for (Reservation reservation : expiredReservations) {
            try {
                reservationService.processSingleExpiration(reservation);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to expire reservation ID: {}. Reason: {}", reservation.getId(), e.getMessage());
            }
        }
        
        log.info("Successfully expired {}/{} reservations.", successCount, expiredReservations.size());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleExpiration(Reservation reservation) {
        Flight flight = reservation.getFlight();

        if (flight == null) {
            log.error("Data Integrity Violation: Reservation ID {} lacks a valid Flight. Skipping.", reservation.getId());
            return;
        }

        flight.setAvailableSeats(flight.getAvailableSeats() + reservation.getNumberOfPassengers());
        
        reservation.setStatus(ReservationStatus.EXPIRED);

        reservationRepository.save(reservation);
        flightRepository.save(flight); 

        log.debug("Reservation {} expired. Seats restored to flight ID: {}", 
                reservation.getReservationCode(), flight.getId());
    } 

}