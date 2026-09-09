package com.aerolinea.flight_booking_api.mappers;

import com.aerolinea.flight_booking_api.dtos.booking.BookingDTO;
import com.aerolinea.flight_booking_api.dtos.flight.FlightSegmentDTO;
import com.aerolinea.flight_booking_api.dtos.itinerary.ItineraryDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.aerolinea.flight_booking_api.models.FlightSegment;
import com.aerolinea.flight_booking_api.models.Itinerary;
import com.aerolinea.flight_booking_api.models.Reservation;

    @Mapper(componentModel = "spring", uses = {ReferenceMapper.class},
            unmappedTargetPolicy = ReportingPolicy.IGNORE)
    public interface BookingMapper {

        @Mapping(source = "user.id", target = "userId")
        BookingDTO toBookingDTO(Reservation reservation);

        ItineraryDTO toItineraryDTO(Itinerary itinerary);

        @Mapping(source = "flightInstance.id", target = "flightInstanceId")
        @Mapping(source = "flightInstance.flightSchedule.flightNumber", target = "flightNumber")
        @Mapping(source = "departureAirport.code", target = "departureAirport")
        @Mapping(source = "arrivalAirport.code", target = "arrivalAirport")
        FlightSegmentDTO toFlightSegmentDTO(FlightSegment segment);
    }
