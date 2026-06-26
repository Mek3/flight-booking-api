package com.aerolinea.flight_booking_api.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.aerolinea.flight_booking_api.dtos.FlightDTO;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.aerolinea.flight_booking_api.exceptions.ResourceNotFoundException;
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
        return flightMapper.toFlightDTO(flightRepository.save(flightMapper.toFlight(flightDTO)));
    }
    

    @Override
    public FlightDTO updateFlight(Long id, FlightDTO flightDTO) {
        Flight existingFlight = flightRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.FLIGHT_NOT_FOUND,"Flight not found with ID: " + id));

        flightMapper.updateFlightFromDTO(flightDTO, existingFlight);

        return flightMapper.toFlightDTO(flightRepository.save(existingFlight));
    }

    @Override
    public FlightDTO flightById(Long id) {

        return flightMapper.toFlightDTO(flightRepository.findById(id).orElseThrow(() ->  
                new ResourceNotFoundException(ErrorCode.FLIGHT_NOT_FOUND,
                                        "Flight not found with ID: " + id)));

    }

    @Override
    public Page<FlightDTO> getFlights(Pageable pageable) {
        return flightRepository.findAll(pageable).map(flightMapper::toFlightDTO);
    }

    @Override
    public void deleteFlightById(Long id) {
        if(!flightRepository.existsById(id)) {
            throw new ResourceNotFoundException(ErrorCode.FLIGHT_NOT_FOUND,
                                        "Flight not found with ID: " + id);
        }
        flightRepository.deleteById(id);
    }


}
