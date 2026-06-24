package com.aerolinea.flight_booking_api.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.aerolinea.flight_booking_api.dtos.FlightDTO;
import com.aerolinea.flight_booking_api.models.Flight;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FlightMapper {

    Flight toFlight(FlightDTO flightDTO);

    FlightDTO toFlightDTO(Flight flight);

    @Mapping(target = "id", ignore = true)
    void updateFlightFromDTO(FlightDTO flightDTO, @MappingTarget Flight flight);
}

