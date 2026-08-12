package com.aerolinea.flight_booking_api.domain.seat;

import com.aerolinea.flight_booking_api.config.AbstractJpaTest;
import com.aerolinea.flight_booking_api.models.*;
import com.aerolinea.flight_booking_api.repositories.SeatRepository;
import com.aerolinea.flight_booking_api.utils.DatabaseTestFixture;
import com.aerolinea.flight_booking_api.utils.SeatTestQueries;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class SeatRepositoryJpaTest extends AbstractJpaTest {

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private DatabaseTestFixture dbFixture;
    private SeatTestQueries seatTestQueries;

    @BeforeEach
    void setUp() {
        dbFixture = new DatabaseTestFixture(entityManager);
        seatTestQueries = new SeatTestQueries(jdbcTemplate);
    }

    @Test
    void shouldInsertOneSeatPerRowAndLetterCombination() {
        FlightInstance flightInstance = dbFixture.persistValidFlightInstance("IBE-SEAT");

        int[] results = seatRepository.batchInsertSeats(flightInstance.getId(), 5, "ABC");

        assertThat(results).hasSize(15);
        assertThat(seatTestQueries.countSeatsFor(flightInstance.getId())).isEqualTo(15);
    }

    @Test
    void shouldBeIdempotentWhenCalledTwiceForTheSameFlightInstance() {
        FlightInstance flightInstance = dbFixture.persistValidFlightInstance("IBE-SEAT");

        seatRepository.batchInsertSeats(flightInstance.getId(), 5, "ABC");
        seatRepository.batchInsertSeats(flightInstance.getId(), 5, "ABC");

        assertThat(seatTestQueries.countSeatsFor(flightInstance.getId()))
                .as("The second call should not create duplicates thanks to INSERT IGNORE + the real UNIQUE constraint")
                .isEqualTo(15);
    }


}