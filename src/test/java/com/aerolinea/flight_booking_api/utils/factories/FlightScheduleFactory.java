package com.aerolinea.flight_booking_api.utils.factories;

import com.aerolinea.flight_booking_api.models.Airport;
import com.aerolinea.flight_booking_api.models.FlightSchedule;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;

public class FlightScheduleFactory {


    public static FlightSchedule.FlightScheduleBuilder validScheduleBuilder(Airport departure, Airport arrival) {
        return FlightSchedule.builder()
                .flightNumber("TEST1234")
                .departureAirport(departure)
                .arrivalAirport(arrival)
                .departureTime(LocalTime.of(8, 0))
                .arrivalTime(LocalTime.of(10, 30))
                .daysOfWeekMask(127);
    }

    public static List<FlightSchedule> generateSchedules(int count, Airport departure, Airport arrival) {
        return IntStream.range(0, count)
                .mapToObj(i -> FlightSchedule.builder()
                        .flightNumber("IBE" + String.format("%04d", i))
                        .departureAirport(departure)
                        .arrivalAirport(arrival)
                        .departureTime(LocalTime.of(8, 0))
                        .arrivalTime(LocalTime.of(10, 30))
                        .daysOfWeekMask(i % 127 + 1)
                        .build())
                .toList();
    }
}
