package com.aerolinea.flight_booking_api.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.aerolinea.flight_booking_api.dtos.FlightDTO;
import com.aerolinea.flight_booking_api.dtos.FlightSearchCriteria;


public interface FlightService {

    public FlightDTO save(FlightDTO flightDTO);
    public FlightDTO updateFlight(Long id, FlightDTO flightDTO);
    public FlightDTO flightById(Long id);
    public void deleteFlightById(Long id);
    public Page<FlightDTO> getFlights(Pageable pageable);
    Page<FlightDTO> searchFlights(FlightSearchCriteria flightSearchCriteria, Pageable pageable);

}
