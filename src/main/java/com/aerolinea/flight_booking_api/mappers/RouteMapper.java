package com.aerolinea.flight_booking_api.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.aerolinea.flight_booking_api.dtos.route.RouteRequest;
import com.aerolinea.flight_booking_api.dtos.route.RouteResponse;
import com.aerolinea.flight_booking_api.models.Route;

@Mapper(
    componentModel = "spring", 
    uses = {ReferenceMapper.class, AirportMapper.class}, 
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface RouteMapper {

    @Mapping(source = "originAirportId", target = "originAirport")
    @Mapping(source = "destinationAirportId", target = "destinationAirport")
    Route toRoute(RouteRequest request);

    RouteResponse toRouteResponse(Route route);

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "originAirportId", target = "originAirport")
    @Mapping(source = "destinationAirportId", target = "destinationAirport")
    void updateRouteFromRequest(RouteRequest request, @MappingTarget Route route);
    
}