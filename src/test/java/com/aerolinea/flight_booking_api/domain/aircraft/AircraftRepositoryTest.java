package com.aerolinea.flight_booking_api.domain.aircraft;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.assertThat;

import com.aerolinea.flight_booking_api.config.AbstractJpaTest;
import com.aerolinea.flight_booking_api.models.Aircraft;
import com.aerolinea.flight_booking_api.models.AircraftModel;
import com.aerolinea.flight_booking_api.repositories.AircraftRepository;

public class AircraftRepositoryTest extends AbstractJpaTest {

    @Autowired
    private AircraftRepository aircraftRepository;

    private AircraftModel createAndPersistAircraftModel() {
        AircraftModel model = AircraftModel.builder()
                .manufacturer("Airbus")
                .modelName("A320neo")
                .maxCapacity((short) 180)
                .build();
        return entityManager.persistAndFlush(model);
    }

    @Test
    void shouldPersistAircraftAndPopulateCreationAuditFields() {
        AircraftModel model = createAndPersistAircraftModel();

        Aircraft aircraft = Aircraft.builder()
                .registrationNumber("EC-MVD")
                .totalFlightHours(1200)
                .aircraftModel(model)
                .build();
        
        Aircraft savedAircraft = entityManager.persistAndFlush(aircraft);
        entityManager.clear();
    
        Aircraft retrieved = aircraftRepository.findById(savedAircraft.getId()).orElseThrow();
        assertThat(retrieved.getRegistrationNumber()).isEqualTo("EC-MVD");
        assertThat(retrieved.getAircraftModel().getId()).isEqualTo(model.getId());
        assertThat(retrieved.getCreatedBy()).isNotNull();
        assertThat(retrieved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldUpdateAircraftAndPopulateModificationAuditFields() {
        AircraftModel model = createAndPersistAircraftModel();

        Aircraft aircraft = Aircraft.builder()
                .registrationNumber("EC-UPD")
                .totalFlightHours(5000)
                .aircraftModel(model)
                .build();
        
        Aircraft savedAircraft = entityManager.persistAndFlush(aircraft);
        entityManager.clear();

        Aircraft retrievedToUpdate = aircraftRepository.findById(savedAircraft.getId()).orElseThrow();
        retrievedToUpdate.setTotalFlightHours(5500);

        aircraftRepository.saveAndFlush(retrievedToUpdate);
        entityManager.clear();

        Aircraft fullyUpdated = aircraftRepository.findById(savedAircraft.getId()).orElseThrow();
        assertThat(fullyUpdated.getTotalFlightHours()).isEqualTo(5500);
        assertThat(fullyUpdated.getUpdatedAt()).isNotNull();
        assertThat(fullyUpdated.getUpdatedBy()).isNotNull();
    }

    @Test
    void shouldSoftDeleteAircraftAndFilterFromFindQueries() {
        AircraftModel model = createAndPersistAircraftModel();

        Aircraft aircraft = Aircraft.builder()
                .registrationNumber("EC-DEL")
                .totalFlightHours(8000)
                .aircraftModel(model)
                .build();
        
        Aircraft savedAircraft = entityManager.persistAndFlush(aircraft);
        entityManager.clear();

        Aircraft retrievedToDelete = aircraftRepository.findById(savedAircraft.getId()).orElseThrow();
        retrievedToDelete.markAsDeleted("system_admin"); 
        
        aircraftRepository.saveAndFlush(retrievedToDelete);
        entityManager.clear();

        assertThat(aircraftRepository.findById(savedAircraft.getId())).isEmpty();
    }
    
}
