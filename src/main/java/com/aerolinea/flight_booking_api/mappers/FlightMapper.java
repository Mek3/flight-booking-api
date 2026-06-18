package com.aerolinea.flight_booking_api.mappers;

import org.springframework.stereotype.Component;

import com.aerolinea.flight_booking_api.dtos.FlightDTO;
import com.aerolinea.flight_booking_api.models.Flight;

@Component
public class FlightMapper {

public FlightDTO toDto(Flight entity) {
        if (entity == null) {
            return null;
        }

        FlightDTO dto = new FlightDTO();
        dto.setId(entity.getId());
        dto.setFlightNumber(entity.getFlightNumber());
        dto.setDeparture(entity.getDeparture());
        dto.setDepartureTime(entity.getDepartureTime());
        dto.setDestination(entity.getDestination());
        dto.setDestinationTime(entity.getDestinationTime());
        dto.setAvailableSeats(entity.getAvailableSeats());
        dto.setPrice(entity.getPrice());

        return dto;
    }

    public Flight toEntity(FlightDTO dto) {
        if (dto == null) {
            return null;
        }

        Flight entity = new Flight();
  
        entity.setId(dto.getId()); 
        entity.setFlightNumber(dto.getFlightNumber());
        entity.setDeparture(dto.getDeparture());
        entity.setDepartureTime(dto.getDepartureTime());
        entity.setDestination(dto.getDestination());
        entity.setDestinationTime(dto.getDestinationTime());
        entity.setAvailableSeats(dto.getAvailableSeats());
        entity.setPrice(dto.getPrice());

        return entity;
    }

}
