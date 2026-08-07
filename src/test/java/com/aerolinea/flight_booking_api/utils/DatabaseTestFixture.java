package com.aerolinea.flight_booking_api.utils;

import com.aerolinea.flight_booking_api.models.Airport;
import com.aerolinea.flight_booking_api.models.FlightSchedule;
import com.aerolinea.flight_booking_api.utils.factories.AirportFactory;
import com.aerolinea.flight_booking_api.utils.factories.FlightScheduleFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

public class DatabaseTestFixture {

    private final TestEntityManager entityManager;

    public DatabaseTestFixture(TestEntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void persistValidSchedule(String flightNumber) {
        Airport departure = entityManager.persist(AirportFactory.validAirportBuilder("MAD").build());
        Airport arrival = entityManager.persist(AirportFactory.validAirportBuilder("JFK").build());

        FlightSchedule schedule = FlightScheduleFactory.validScheduleBuilder(departure, arrival)
                .flightNumber(flightNumber)
                .build();

        entityManager.persistAndFlush(schedule);
    }

    public void persistMultipleSchedules(int count) {
        Airport departure = entityManager.persist(AirportFactory.validAirportBuilder("MAD").build());
        Airport arrival = entityManager.persist(AirportFactory.validAirportBuilder("JFK").build());

        List<FlightSchedule> schedules = FlightScheduleFactory.generateSchedules(count, departure, arrival);

        for (FlightSchedule schedule : schedules) {
            entityManager.persist(schedule);
        }
        entityManager.flush();
    }
}
