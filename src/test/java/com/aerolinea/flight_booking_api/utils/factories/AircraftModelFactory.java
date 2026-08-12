package com.aerolinea.flight_booking_api.utils.factories;

import com.aerolinea.flight_booking_api.models.AircraftModel;

public class AircraftModelFactory {

    public static AircraftModel.AircraftModelBuilder validModelBuilder() {
        return AircraftModel.builder()
                .manufacturer("Airbus")
                .modelName("A320")
                .maxCapacity((short) 180);
    }
}