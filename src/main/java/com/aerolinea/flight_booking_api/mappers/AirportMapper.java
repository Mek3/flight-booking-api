package com.aerolinea.flight_booking_api.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.aerolinea.flight_booking_api.dtos.airport.AirportRequest;
import com.aerolinea.flight_booking_api.dtos.airport.AirportResponse;
import com.aerolinea.flight_booking_api.models.Airport;

@Mapper(componentModel = "spring", uses = {ReferenceMapper.class}, 
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AirportMapper {

    AirportResponse toAirportResponse(Airport airport);

    Airport toAirport(AirportResponse airportResponse);

    Airport toAirport(AirportRequest airportRequest);

    AirportRequest toAirportRequest(Airport airport);
    
    @Mapping(target = "id", ignore= true)
    void updateAirportFromRecord(AirportRequest airportRequest, @MappingTarget Airport airport);
}