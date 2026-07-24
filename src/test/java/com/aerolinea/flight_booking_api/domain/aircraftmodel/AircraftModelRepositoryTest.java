package com.aerolinea.flight_booking_api.domain.aircraftmodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.aerolinea.flight_booking_api.config.AbstractIntegrationTest;
import com.aerolinea.flight_booking_api.config.JpaConfig;
import com.aerolinea.flight_booking_api.models.AircraftModel;
import com.aerolinea.flight_booking_api.repositories.AircraftModelRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
public class AircraftModelRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private AircraftModelRepository aircraftModelRepository;

   @Autowired
    private TestEntityManager entityManagerTest;

    @MockitoBean(name="auditorAware")
    private AuditorAware<String> auditorAware;

    @BeforeEach
    void setUp(){
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("System_test"));
    }

    @Test
    void shouldPersisteAircraftModelAndPopulateCreationAuditFields(){
        AircraftModel anotherAircraft = AircraftModel.builder()
                .manufacturer("Airbus")
                .modelName("A320neo")
                .maxCapacity((short) 195)
                .build();

        AircraftModel aircraftModelSaved = entityManagerTest.persistAndFlush(anotherAircraft);
        entityManagerTest.clear();

        assertThat(aircraftModelSaved).isNotNull();

        AircraftModel aircraftModel = aircraftModelRepository.findById(aircraftModelSaved.getId()).orElseThrow();
        
        assertThat(aircraftModel.getManufacturer()).isEqualTo("Airbus");
        assertThat(aircraftModel.getCreatedBy()).isEqualTo("System_test");
        assertThat(aircraftModel.getCreatedAt()).isNotNull();

    }

    @Test
    void shouldUpdateAircraftModelAndPopulateUpdateAuditFields(){
        AircraftModel anotherAircraft = AircraftModel.builder()
                .manufacturer("Airbus")
                .modelName("A320neo")
                .maxCapacity((short) 195)
                .build();

        AircraftModel aircraftModelSaved = entityManagerTest.persistAndFlush(anotherAircraft);
        entityManagerTest.clear();

        assertThat(aircraftModelSaved).isNotNull();

        AircraftModel aircraftModel = aircraftModelRepository.findById(aircraftModelSaved.getId()).orElseThrow();
        aircraftModel.setManufacturer("Airbus updated");

        entityManagerTest.flush();
        entityManagerTest.clear();

        AircraftModel aircraftModelResult = aircraftModelRepository.findById(aircraftModelSaved.getId()).orElseThrow();

        assertThat(aircraftModelResult.getManufacturer()).isEqualTo("Airbus updated");
        assertThat(aircraftModelResult.getUpdatedBy()).isEqualTo("System_test");
        assertThat(aircraftModelResult.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldDeleteAircraftModelAndPopulateDeletionAuditFields(){
        AircraftModel anotherAircraft = AircraftModel.builder()
                .manufacturer("Airbus")
                .modelName("A320neo")
                .maxCapacity((short) 195)
                .build();

        AircraftModel aircraftModelSaved = entityManagerTest.persistAndFlush(anotherAircraft);
        entityManagerTest.clear();

        assertThat(aircraftModelSaved).isNotNull();

        AircraftModel aircraftModel = aircraftModelRepository.findById(aircraftModelSaved.getId()).orElseThrow();
        aircraftModel.markAsDeleted("system_test");

        entityManagerTest.flush();
        entityManagerTest.clear();

        Object[] aircraftModelResult = (Object[]) entityManagerTest.getEntityManager()
            .createNativeQuery("SELECT deleted_by, deleted_at FROM aircraft_models WHERE id = :id")
            .setParameter("id", aircraftModelSaved.getId())
            .getSingleResult();
       
        assertThat(aircraftModelResult[0]).isEqualTo("system_test");
        assertThat(aircraftModelResult[1]).isNotNull();

    }










}
