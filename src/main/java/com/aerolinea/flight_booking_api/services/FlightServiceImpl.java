package com.aerolinea.flight_booking_api.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.aerolinea.flight_booking_api.dtos.FlightDTO;
import com.aerolinea.flight_booking_api.mappers.FlightMapper;
import com.aerolinea.flight_booking_api.models.Flight;
import com.aerolinea.flight_booking_api.repositories.FlightRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class FlightServiceImpl implements FlightService{

    private final FlightRepository flightRepository;
    private final FlightMapper flightMapper;

    @Override
    public FlightDTO save(FlightDTO flightDTO) {
        return flightMapper.toDto(flightRepository.save(flightMapper.toEntity(flightDTO)));
    }

    

    @Override
    public FlightDTO updateFlight(Long id, FlightDTO flightDTO) {
        Flight existingFlight = flightRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Flight not found with ID: " + id));

        existingFlight.setFlightNumber(flightDTO.getFlightNumber());
        existingFlight.setDeparture(flightDTO.getDeparture());
        existingFlight.setDepartureTime(flightDTO.getDepartureTime());
        existingFlight.setDestination(flightDTO.getDestination());
        existingFlight.setDestinationTime(flightDTO.getDestinationTime());
        existingFlight.setAvailableSeats(flightDTO.getAvailableSeats());
        existingFlight.setPrice(flightDTO.getPrice());

        return flightMapper.toDto(flightRepository.save(existingFlight));
    }

    @Override
    public FlightDTO flightById(Long id) {

        return flightMapper.toDto(flightRepository.findById(id).orElseThrow(() ->  new IllegalArgumentException("Flight not found with ID: " + id)));

    }

    @Override
    public List<FlightDTO> getFlights() {
        return flightRepository.findAll().stream().map(flight -> flightMapper.toDto(flight)).toList();
    }



    @Override
    public void deleteFlightById(Long id) {
        if(!flightRepository.existsById(id)) {
            throw new IllegalArgumentException("Flight not found with ID: " + id);
        }
        flightRepository.deleteById(id);
    }


}
