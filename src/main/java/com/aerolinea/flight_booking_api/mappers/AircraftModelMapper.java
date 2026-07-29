package com.aerolinea.flight_booking_api.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.aerolinea.flight_booking_api.dtos.aircraftmodel.AircraftModelRequest;
import com.aerolinea.flight_booking_api.dtos.aircraftmodel.AircraftModelResponse;
import com.aerolinea.flight_booking_api.models.AircraftModel;

@Mapper(componentModel = "spring", uses = ReferenceMapper.class, 
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AircraftModelMapper {

    AircraftModel toAircraftModel(AircraftModelRequest aircraftModelRequest);
    
    AircraftModelRequest toAircraftModelRequest(AircraftModel aircraftModel);

    AircraftModelResponse toAircraftModelResponse(AircraftModel aircraftModel);

    @Mapping(target = "id", ignore = true)
    void updateAircraftModelFromAircraftModelRequest(AircraftModelRequest aircraftModelRequest, @MappingTarget AircraftModel aircraftModel);

}
