package com.aerolinea.flight_booking_api.utils.factories;

import com.aerolinea.flight_booking_api.models.AircraftLayout;
import com.aerolinea.flight_booking_api.models.Airport;
import com.aerolinea.flight_booking_api.models.FlightSchedule;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;

public class FlightScheduleFactory {


    public static FlightSchedule.FlightScheduleBuilder validScheduleBuilder(Airport departure, Airport arrival, AircraftLayout aircraftLayout) {
        return FlightSchedule.builder()
                .flightNumber("TEST1234")
                .departureAirport(departure)
                .arrivalAirport(arrival)
                .departureTime(LocalTime.of(8, 0))
                .arrivalTime(LocalTime.of(10, 30))
                .arrivalDayOffset(0)
                .basePrice(new BigDecimal("199.99"))
                .daysOfWeekMask(127)
                .aircraftLayout(aircraftLayout);
    }

    public static List<FlightSchedule> generateSchedules(int count, Airport departure, Airport arrival, AircraftLayout aircraftLayout) {
        return IntStream.range(0, count)
                .mapToObj(i -> validScheduleBuilder(departure, arrival, aircraftLayout)
                        .flightNumber("IBE" + String.format("%04d", i))
                        .daysOfWeekMask(i % 127 + 1)
                        .build())
                .toList();
    }

}