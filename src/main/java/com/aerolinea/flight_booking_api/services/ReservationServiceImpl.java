package com.aerolinea.flight_booking_api.services;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class ReservationServiceImpl implements ReservationService{

    private final UserRepository userRepository;
    private final FlightRepository flightRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationMapper reservationMapper;


    @Override
    @Transactional
    public ReservationDTO createReservation(ReservationRequest reservationRequest) {
        if(reservationRequest == null) {
            throw new IllegalArgumentException("ReservationDTO is null");
        }

        Reservation reservation = new Reservation();

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByUsername(username)
                                    .orElseThrow(()-> {
                                        throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "User not found with username: " + username);
                                     });
        
        reservation.setUser(user);

        Flight flight = flightRepository.findById(reservationRequest.flightId())
                                        .orElseThrow(() -> 
                                        new ResourceNotFoundException(ErrorCode.FLIGHT_NOT_FOUND,
                                             "Flight not found with ID: " + reservationRequest.flightId()));

        if(flight.getAvailableSeats() < reservationRequest.numberOfPassengers()) {
            throw new BusinessRuleViolationException(ErrorCode.NOT_ENOUGH_SEATS, "Not enough seats available for this flight with ID: " + reservationRequest.flightId());
        }

        flight.setAvailableSeats(flight.getAvailableSeats() - reservationRequest.numberOfPassengers());
        reservation.setFlight(flight);

        BigDecimal totalPrice =flight.getPrice().multiply(BigDecimal.valueOf(reservationRequest.numberOfPassengers()));
        reservation.setTotalPrice(totalPrice);  

        reservation.setNumberOfPassengers(reservationRequest.numberOfPassengers());

        reservation.setStatus(ReservationStatus.CONFIRMED);

        String uuid = UUID.randomUUID().toString().substring(0,8).toUpperCase();
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

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
    
        if(username == null) {
            throw new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND,
                 "User not found with username: " + username);
        }

        Reservation reservation = reservationRepository.findById(idReservation).orElseThrow(() -> 
                new ResourceNotFoundException(ErrorCode.RESERVATION_NOT_FOUND,
                 "Reservation not found with ID: " + idReservation));

        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = reservation.getUser() != null && reservation.getUser().getUsername().equals(username);

        if(!isAdmin && !isOwner) {
            throw new BusinessRuleViolationException(ErrorCode.INSUFFICIENT_PERMISSIONS,
                     "User " + username +" lacks permissions to cancel Reservation ID: " + idReservation);
        }

        if(reservation.getStatus().equals(ReservationStatus.CANCELLED)) {
            throw new BusinessRuleViolationException(ErrorCode.RESERVATION_ALREADY_CANCELLED,
                 "Reservation  with ID: " + idReservation + " already cancelled.");
        }

        if(reservation.getFlight()== null) {
            throw new ResourceNotFoundException(ErrorCode.FLIGHT_NOT_FOUND,
                 "Flight not found in Reservation with ID: " + idReservation);
        }
            
        Duration diff = Duration.between(LocalDateTime.now(), reservation.getFlight().getDepartureTime());
        if(diff.toHours() <= 24) {
            throw new BusinessRuleViolationException(ErrorCode.CANCELLATION_TIME_EXPIRED, 
                "Cancellations must be made at least 24 hours in advance. ID: " + idReservation);
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.getFlight().setAvailableSeats(reservation.getFlight().getAvailableSeats() + reservation.getNumberOfPassengers());
        reservationRepository.save(reservation);
    }




}
