package com.aerolinea.flight_booking_api.services;

import com.aerolinea.flight_booking_api.dtos.flight.FlightSearchResultDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.aerolinea.flight_booking_api.dtos.FlightSearchCriteria;


public interface FlightService {

    Page<FlightSearchResultDTO> searchFlights(FlightSearchCriteria flightSearchCriteria, Pageable pageable);

}
