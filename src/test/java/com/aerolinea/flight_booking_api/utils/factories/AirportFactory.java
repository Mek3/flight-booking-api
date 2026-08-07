package com.aerolinea.flight_booking_api.utils.factories;

import com.aerolinea.flight_booking_api.models.Airport;

public class AirportFactory {
    public static Airport.AirportBuilder validAirportBuilder(String code) {
        return Airport.builder()
                .name("Test Airport " + code)
                .code(code)
                .city("Mock City")
                .country("Mock Country");
    }
}
