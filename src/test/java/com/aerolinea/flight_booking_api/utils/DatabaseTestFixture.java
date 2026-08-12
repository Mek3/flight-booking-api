package com.aerolinea.flight_booking_api.utils;

import com.aerolinea.flight_booking_api.models.*;
import com.aerolinea.flight_booking_api.models.enums.FlightStatus;
import com.aerolinea.flight_booking_api.utils.factories.*;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;

public class DatabaseTestFixture {

    private final TestEntityManager entityManager;

    public DatabaseTestFixture(TestEntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public FlightSchedule persistValidSchedule(String flightNumber) {
        Airport departure = entityManager.persist(AirportFactory.validAirportBuilder("MAD").build());
        Airport arrival = entityManager.persist(AirportFactory.validAirportBuilder("JFK").build());
        AircraftModel aircrafmodel = entityManager.persist(AircraftModelFactory.validModelBuilder().build());
        AircraftLayout aircraftLayout = entityManager.persist(AircraftLayoutFactory.validLayoutBuilder(aircrafmodel).build());

        FlightSchedule schedule = FlightScheduleFactory.validScheduleBuilder(departure, arrival, aircraftLayout)
                .flightNumber(flightNumber)
                .build();

       return  entityManager.persistAndFlush(schedule);
    }

    public void persistMultipleSchedules(int count) {
        Airport departure = entityManager.persist(AirportFactory.validAirportBuilder("MAD").build());
        Airport arrival = entityManager.persist(AirportFactory.validAirportBuilder("JFK").build());
        AircraftModel aircrafmodel = entityManager.persist(AircraftModelFactory.validModelBuilder().build());
        AircraftLayout aircraftLayout = entityManager.persist(AircraftLayoutFactory.validLayoutBuilder(aircrafmodel).build());

        List<FlightSchedule> schedules = FlightScheduleFactory.generateSchedules(count, departure, arrival, aircraftLayout);

        for (FlightSchedule schedule : schedules) {
            entityManager.persist(schedule);
        }
        entityManager.flush();
    }

    public FlightInstance persistValidFlightInstance(String flightNumber) {
        FlightSchedule schedule = persistValidSchedule(flightNumber);

        FlightInstance flightInstance = FlightInstanceFactory.validInstanceBuilder(schedule).build();

        return entityManager.persistAndFlush(flightInstance);
    }
}
