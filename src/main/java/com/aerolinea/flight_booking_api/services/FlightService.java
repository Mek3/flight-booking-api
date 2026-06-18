package com.aerolinea.flight_booking_api.services;

import java.util.List;

import com.aerolinea.flight_booking_api.dtos.FlightDTO;


public interface FlightService {

    public FlightDTO save(FlightDTO flightDTO);
    public FlightDTO updateFlight(Long id, FlightDTO flightDTO);
    public FlightDTO flightById(Long id);
    public void deleteFlightById(Long id);
    public List<FlightDTO> getFlights();

}
