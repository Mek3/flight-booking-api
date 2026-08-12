package com.aerolinea.flight_booking_api.utils.factories;

import com.aerolinea.flight_booking_api.models.AircraftLayout;
import com.aerolinea.flight_booking_api.models.AircraftModel;

import java.util.List;
import java.util.stream.IntStream;

public class AircraftLayoutFactory {

    public static AircraftLayout.AircraftLayoutBuilder validLayoutBuilder(AircraftModel aircraftModel) {
        return AircraftLayout.builder()
                .aircraftModel(aircraftModel)
                .cabinClass("ECONOMY")
                .totalRows(30)
                .seatLetters("ABCDEF")
                .seatCapacity(180);
    }

    public static List<AircraftLayout> generateLayouts(int count, AircraftModel aircraftModel) {
        return IntStream.range(0, count)
                .mapToObj(i -> {
                    int rows = 20 + i;
                    return AircraftLayout.builder()
                            .aircraftModel(aircraftModel)
                            .cabinClass("ECONOMY")
                            .totalRows(rows)
                            .seatLetters("ABCDEF")
                            .seatCapacity(rows * 6)
                            .build();
                })
                .toList();
    }
}