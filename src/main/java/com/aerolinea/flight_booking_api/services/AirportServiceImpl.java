package com.aerolinea.flight_booking_api.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aerolinea.flight_booking_api.dtos.airport.AirportRequest;
import com.aerolinea.flight_booking_api.dtos.airport.AirportResponse;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.aerolinea.flight_booking_api.exceptions.ResourceNotFoundException;
import com.aerolinea.flight_booking_api.mappers.AirportMapper;
import com.aerolinea.flight_booking_api.models.Airport;
import com.aerolinea.flight_booking_api.repositories.AirportRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AirportServiceImpl implements AirportService {

    private final AirportRepository airportRepository;
    private final AirportMapper airportMapper;

    @Override
    public AirportResponse getAirportById(Long id) {
        return airportMapper.toAirportResponse(airportRepository.findById(id).orElseThrow(() -> 
                new ResourceNotFoundException(ErrorCode.AIRPORT_NOT_FOUND, 
                    String.format(ErrorCode.AIRPORT_NOT_FOUND.getMessage(), id))));
    }

    @Override
    public Page<AirportResponse> getAllAirports(Pageable pageable) {
        return airportRepository.findAll(pageable).map(airportMapper::toAirportResponse);
    }

    @Override
    @Transactional
    public AirportResponse createAirport(AirportRequest airportRequest) {
        Airport airport = airportRepository.save(airportMapper.toAirport(airportRequest));
     
        log.info("Airport created successfully with ID: {}", airport.getId());
        return airportMapper.toAirportResponse(airport);
    }

    @Override
    @Transactional
    public AirportResponse updateAirport(Long id, AirportRequest airportRequest) {
        Airport airport = airportRepository.findById(id).orElseThrow(() -> 
                        new ResourceNotFoundException(ErrorCode.AIRPORT_NOT_FOUND, 
                            String.format(ErrorCode.AIRPORT_NOT_FOUND.getMessage(), id)));
        
        airportMapper.updateAirportFromRecord(airportRequest, airport);
        airportRepository.save(airport);

        log.info("Airport updated successfully with ID: {}", airport.getId());

        return airportMapper.toAirportResponse(airport);
    }

    @Override
    @Transactional
    public void deleteAirport(Long id) {
        if(!airportRepository.existsById(id)){
             throw new ResourceNotFoundException(ErrorCode.AIRPORT_NOT_FOUND, 
                String.format(ErrorCode.AIRPORT_NOT_FOUND.getMessage(), id));
        }
        airportRepository.deleteById(id);
        
        log.info("Airport deleted successfully with ID: {}", id);
    }

}
