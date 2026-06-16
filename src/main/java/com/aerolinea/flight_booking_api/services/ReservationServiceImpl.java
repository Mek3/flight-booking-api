package com.aerolinea.flight_booking_api.services;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aerolinea.flight_booking_api.dtos.ReservationDTO;
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


    @Override
    @Transactional
    public ReservationDTO createReservation(ReservationDTO reservationDTO) {
       
        if(reservationDTO == null) {
            throw new IllegalArgumentException("ReservationDTO is null");
        }

        Reservation reservation = new Reservation();

        User user = userRepository.findById(reservationDTO.getUserId())
                                    .orElseThrow(()-> new IllegalArgumentException("User not found with ID: " + reservationDTO.getUserId()));
        
        reservation.setUser(user);

        Flight flight = flightRepository.findById(reservationDTO.getFlightId())
                                        .orElseThrow(() -> new IllegalArgumentException("Flight not found with ID: " + reservationDTO.getFlightId()));

        if(flight.getAvaibleSeats() < reservationDTO.getNumberOfPassengers()) {
            throw new RuntimeException("There aren't seats avaible");
        }

        flight.setAvaibleSeats(flight.getAvaibleSeats() - reservationDTO.getNumberOfPassengers());
        reservation.setFlight(flight);

        BigDecimal totalPrice =flight.getPrice().multiply(BigDecimal.valueOf(reservationDTO.getNumberOfPassengers()));
        reservation.setTotalPrice(totalPrice);  

        reservation.setNumberOfPassengers(reservationDTO.getNumberOfPassengers());

        reservation.setStatus(ReservationStatus.CONFIRMED);

        String uuid = UUID.randomUUID().toString().substring(0,8).toUpperCase();
        reservation.setReservationCode(uuid); 

        flightRepository.save(flight);
        reservationRepository.save(reservation);

        reservationDTO.setId(reservation.getId());

        reservationDTO.setId(reservation.getId());
        reservationDTO.setReservationCode(reservation.getReservationCode());
        reservationDTO.setStatus(reservation.getStatus());
        reservationDTO.setTotalPrice(reservation.getTotalPrice());
        reservationDTO.setCreatedAt(reservation.getCreatedAt());

        return reservationDTO;
    }





}
