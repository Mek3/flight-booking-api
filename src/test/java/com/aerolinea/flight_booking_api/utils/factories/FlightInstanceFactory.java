package com.aerolinea.flight_booking_api.utils.factories;

import com.aerolinea.flight_booking_api.models.FlightInstance;
import com.aerolinea.flight_booking_api.models.FlightSchedule;
import com.aerolinea.flight_booking_api.models.enums.FlightStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

public class FlightInstanceFactory {

    public static FlightInstance.FlightInstanceBuilder validInstanceBuilder(FlightSchedule flightSchedule) {
        return FlightInstance.builder()
                .flightSchedule(flightSchedule)
                .departureDate(LocalDate.now().plusDays(30))
                .status(FlightStatus.SCHEDULED);
    }

    public static List<FlightInstance> generateInstances(int count, FlightSchedule flightSchedule) {
        return IntStream.range(0, count)
                .mapToObj(i -> FlightInstance.builder()
                        .flightSchedule(flightSchedule)
                        .departureDate(LocalDate.now().plusDays(30 + i))
                        .status(FlightStatus.SCHEDULED)
                        .build())
                .toList();
    }
}