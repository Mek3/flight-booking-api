package com.aerolinea.flight_booking_api.domain.aircraftmodel;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.aerolinea.flight_booking_api.config.AbstractJpaTest;
import com.aerolinea.flight_booking_api.models.AircraftModel;
import com.aerolinea.flight_booking_api.repositories.AircraftModelRepository;

public class AircraftModelRepositoryTest extends AbstractJpaTest {

    @Autowired
    private AircraftModelRepository aircraftModelRepository;


    @Test
    void shouldPersisteAircraftModelAndPopulateCreationAuditFields(){
        AircraftModel anotherAircraft = AircraftModel.builder()
                .manufacturer("Airbus")
                .modelName("A320neo")
                .maxCapacity((short) 195)
                .build();

        AircraftModel aircraftModelSaved = entityManager.persistAndFlush(anotherAircraft);
        entityManager.clear();

        assertThat(aircraftModelSaved).isNotNull();

        AircraftModel aircraftModel = aircraftModelRepository.findById(aircraftModelSaved.getId()).orElseThrow();
        
        assertThat(aircraftModel.getManufacturer()).isEqualTo("Airbus");
        assertThat(aircraftModel.getCreatedBy()).isEqualTo("test_admin");
        assertThat(aircraftModel.getCreatedAt()).isNotNull();

    }

    @Test
    void shouldUpdateAircraftModelAndPopulateUpdateAuditFields(){
        AircraftModel anotherAircraft = AircraftModel.builder()
                .manufacturer("Airbus")
                .modelName("A320neo")
                .maxCapacity((short) 195)
                .build();

        AircraftModel aircraftModelSaved = entityManager.persistAndFlush(anotherAircraft);
        entityManager.clear();

        assertThat(aircraftModelSaved).isNotNull();

        AircraftModel aircraftModel = aircraftModelRepository.findById(aircraftModelSaved.getId()).orElseThrow();
        aircraftModel.setManufacturer("Airbus updated");

        entityManager.flush();
        entityManager.clear();

        AircraftModel aircraftModelResult = aircraftModelRepository.findById(aircraftModelSaved.getId()).orElseThrow();

        assertThat(aircraftModelResult.getManufacturer()).isEqualTo("Airbus updated");
        assertThat(aircraftModelResult.getUpdatedBy()).isEqualTo("test_admin");
        assertThat(aircraftModelResult.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldDeleteAircraftModelAndPopulateDeletionAuditFields(){
        AircraftModel anotherAircraft = AircraftModel.builder()
                .manufacturer("Airbus")
                .modelName("A320neo")
                .maxCapacity((short) 195)
                .build();

        AircraftModel aircraftModelSaved = entityManager.persistAndFlush(anotherAircraft);
        entityManager.clear();

        assertThat(aircraftModelSaved).isNotNull();

        AircraftModel aircraftModel = aircraftModelRepository.findById(aircraftModelSaved.getId()).orElseThrow();
        aircraftModel.markAsDeleted("system_test");

        entityManager.flush();
        entityManager.clear();

        Object[] aircraftModelResult = (Object[]) entityManager.getEntityManager()
            .createNativeQuery("SELECT deleted_by, deleted_at FROM aircraft_models WHERE id = :id")
            .setParameter("id", aircraftModelSaved.getId())
            .getSingleResult();
       
        assertThat(aircraftModelResult[0]).isEqualTo("system_test");
        assertThat(aircraftModelResult[1]).isNotNull();

    }

    @Test
    void shouldExistsAircraftModelByManufacturerAndModelNameReturnTrue(){
        AircraftModel anotherAircraft = AircraftModel.builder()
                .manufacturer("Airbus")
                .modelName("A320neo")
                .maxCapacity((short) 195)
                .build();

        AircraftModel aircraftModelSaved = entityManager.persistAndFlush(anotherAircraft);
        entityManager.clear();

        boolean resultTrue = aircraftModelRepository.existsAircraftModelByManufacturerAndModelName(aircraftModelSaved.getManufacturer(), aircraftModelSaved.getModelName());
        assertThat(resultTrue).isTrue();
       
    }
    
    @Test
    void ShouldExistsAircraftModelByManufacturerAndModelNameReturnFalse(){
       
        Boolean resultFalse = aircraftModelRepository
                        .existsAircraftModelByManufacturerAndModelName("Boing", "737");
        assertThat(resultFalse).isFalse();
    }










}
