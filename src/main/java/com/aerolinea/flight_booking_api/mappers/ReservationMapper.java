package com.aerolinea.flight_booking_api.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.aerolinea.flight_booking_api.dtos.ReservationDTO;
import com.aerolinea.flight_booking_api.dtos.ReservationRequest;
import com.aerolinea.flight_booking_api.models.Reservation;

@Mapper(componentModel="spring", uses = {ReferenceMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReservationMapper {

    @Mapping(source="flightInstance.id", target = "flightInstanceId")
    @Mapping(source="user.id", target = "userId")
    ReservationDTO toReservationDTO(Reservation reservation);

    @Mapping(source="flightInstanceId", target = "flightInstance")
    Reservation toReservation(ReservationRequest reservationRequest);
    
} 