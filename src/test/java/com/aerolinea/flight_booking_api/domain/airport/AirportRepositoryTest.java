package com.aerolinea.flight_booking_api.domain.airport;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

import com.aerolinea.flight_booking_api.config.AbstractIntegrationTest;
import com.aerolinea.flight_booking_api.config.JpaConfig;
import com.aerolinea.flight_booking_api.models.Airport;
import com.aerolinea.flight_booking_api.repositories.AirportRepository;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
public class AirportRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AirportRepository airportRepository;

    @MockitoBean(name = "auditorAware")
    private AuditorAware<String> auditorAware;

    @BeforeEach
    void setupAuditor() {
        Mockito.when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("test_admin"));
    }
     
   @Test
    void shouldPersistAirportAndPopulateCreationAuditFields() {

        Airport airport = Airport.builder()
                .name("Test Airport")
                .code("TST")
                .city("Test City")
                .country("Test Country")
                .build();
        
        Airport savedAirport = entityManager.persistAndFlush(airport);
        entityManager.clear();
    
        Airport retrieved = airportRepository.findById(savedAirport.getId()).orElseThrow();
        assertThat(retrieved.getName()).isEqualTo("Test Airport");
        assertThat(retrieved.getCreatedBy()).isNotNull();
        assertThat(retrieved.getCreatedAt()).isNotNull();

    }

    @Test
    void shouldUpdateAirportAndPopulateModificationAuditFields() {
        Airport airport = Airport.builder()
                .name("Initial Name")
                .code("UPD")
                .city("Test City")
                .country("Test Country")
                .build();
        
        Airport savedAirport = entityManager.persistAndFlush(airport);
        entityManager.clear();

        Airport retrievedToUpdate = airportRepository.findById(savedAirport.getId()).orElseThrow();
        ReflectionTestUtils.setField(retrievedToUpdate, "name", "Updated Name"); 

        airportRepository.saveAndFlush(retrievedToUpdate);
        entityManager.clear();

        Airport fullyUpdated = airportRepository.findById(savedAirport.getId()).orElseThrow();
        assertThat(fullyUpdated.getName()).isEqualTo("Updated Name");
        assertThat(fullyUpdated.getUpdatedAt()).isNotNull();
        assertThat(fullyUpdated.getUpdatedBy()).isNotNull();
    }

    @Test
    void shouldSoftDeleteAirportAndFilterFromFindQueries() {
        Airport airport = Airport.builder()
                .name("To Be Deleted")
                .code("DEL")
                .city("Test City")
                .country("Test Country")
                .build();
        
        Airport savedAirport = entityManager.persistAndFlush(airport);
        entityManager.clear();

        Airport retrievedToDelete = airportRepository.findById(savedAirport.getId()).orElseThrow();
        retrievedToDelete.markAsDeleted("system_admin"); // Execute the domain logic
        airportRepository.saveAndFlush(retrievedToDelete);
        entityManager.clear();

        assertThat(airportRepository.findById(savedAirport.getId())).isEmpty();
    }

}
