package com.aerolinea.flight_booking_api.repositories.projections;

public interface FlightInstanceCountProjection {

    Long getFlightInstanceId();

    Long getTotal();
}
