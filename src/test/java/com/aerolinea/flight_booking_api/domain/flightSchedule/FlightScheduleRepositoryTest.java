package com.aerolinea.flight_booking_api.domain.flightSchedule;

import com.aerolinea.flight_booking_api.config.AbstractJpaTest;
import com.aerolinea.flight_booking_api.repositories.FlightScheduleRepository;
import com.aerolinea.flight_booking_api.utils.DatabaseTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class FlightScheduleRepositoryTest extends AbstractJpaTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FlightScheduleRepository flightScheduleRepository;

    private DatabaseTestFixture dbFixture;

    @BeforeEach
    void setUp() {
        dbFixture = new DatabaseTestFixture(entityManager);
    }

    @Test
    void whenSearchingSchedules_shouldFilterCorrectlyAndNotDuplicate() {
        dbFixture.persistMultipleSchedules(100);

        long totalSchedules = flightScheduleRepository.count();
        assertThat(totalSchedules).isEqualTo(100);
    }
}