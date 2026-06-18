package com.aerolinea.flight_booking_api.services;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aerolinea.flight_booking_api.dtos.ReservationDTO;
import com.aerolinea.flight_booking_api.dtos.ReservationRequest;
import com.aerolinea.flight_booking_api.mappers.ReservationMapper;
import com.aerolinea.flight_booking_api.models.Flight;
import com.aerolinea.flight_booking_api.models.Reservation;
import com.aerolinea.flight_booking_api.models.ReservationStatus;
import com.aerolinea.flight_booking_api.models.User;
import com.aerolinea.flight_booking_api.repositories.FlightRepository;
import com.aerolinea.flight_booking_api.repositories.ReservationRepository;
import com.aerolinea.flight_booking_api.repositories.UserRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
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
                                    .orElseThrow(()-> new IllegalArgumentException("User not found with username: " + username));
        
        reservation.setUser(user);

        Flight flight = flightRepository.findById(reservationRequest.flightId())
                                        .orElseThrow(() -> new IllegalArgumentException("Flight not found with ID: " + reservationRequest.flightId()));

        if(flight.getAvailableSeats() < reservationRequest.numberOfPassengers()) {
            throw new RuntimeException("There aren't seats available");
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

        return reservationMapper.toDto(reservationRepository.saveAndFlush(reservation));

    }





}
