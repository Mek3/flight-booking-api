package com.aerolinea.flight_booking_api.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.aerolinea.flight_booking_api.dtos.aircraft.AircraftRequest;
import com.aerolinea.flight_booking_api.dtos.aircraft.AircraftResponse;
import com.aerolinea.flight_booking_api.models.Aircraft;

@Mapper(componentModel = "spring", uses = ReferenceMapper.class,
   unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AircraftMapper {

    @Mapping(source = "aircraftModelId", target = "aircraftModel")
    Aircraft toAircraft(AircraftRequest request);

    @Mapping(source = "aircraftModel.id", target = "aircraftModelId")
    AircraftResponse toAircraftResponse(Aircraft aircraft);

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "aircraftModelId", target = "aircraftModel")
    void updateAircraftFromRequest(AircraftRequest request, @MappingTarget Aircraft aircraft);
    
}
