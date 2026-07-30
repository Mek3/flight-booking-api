package com.aerolinea.flight_booking_api.domain.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.aerolinea.flight_booking_api.config.AbstractJpaTest;
import com.aerolinea.flight_booking_api.models.Airport;
import com.aerolinea.flight_booking_api.models.Route;
import com.aerolinea.flight_booking_api.repositories.RouteRepository;

public class RouteRepositoryTest extends AbstractJpaTest {

    @Autowired
    private RouteRepository routeRepository;

    private Airport persistValidAirport(String code) {
        Airport airport = Airport.builder()
                .name("Airport " + code)
                .code(code)
                .city("Test City " + code)
                .country("Test Country")
                .build();
        return entityManager.persistAndFlush(airport);
    }

    @Test
    void shouldPersistRouteAndPopulateCreationAuditFields() {
        Airport origin = persistValidAirport("MAD");
        Airport destination = persistValidAirport("JFK");

        Route newRoute = Route.builder()
                .originAirport(origin)
                .destinationAirport(destination)
                .build();

        Route savedRoute = entityManager.persistAndFlush(newRoute);
        entityManager.clear();

        assertThat(savedRoute).isNotNull();
        assertThat(savedRoute.getId()).isNotNull();

        Route retrievedRoute = routeRepository.findById(savedRoute.getId()).orElseThrow();
        
        assertThat(retrievedRoute.getOriginAirport().getId()).isEqualTo(origin.getId());
        assertThat(retrievedRoute.getDestinationAirport().getId()).isEqualTo(destination.getId());
        assertThat(retrievedRoute.getCreatedBy()).isEqualTo("test_admin");
        assertThat(retrievedRoute.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldUpdateRouteAndPopulateUpdateAuditFields() {
        Airport origin = persistValidAirport("MAD");
        Airport destination = persistValidAirport("JFK");
        Airport newDestination = persistValidAirport("LAX");

        Route newRoute = Route.builder()
                .originAirport(origin)
                .destinationAirport(destination)
                .build();

        Route savedRoute = entityManager.persistAndFlush(newRoute);
        entityManager.clear();

        Route retrievedRoute = routeRepository.findById(savedRoute.getId()).orElseThrow();
        retrievedRoute.setDestinationAirport(newDestination);

        entityManager.flush();
        entityManager.clear();

        Route updatedRoute = routeRepository.findById(savedRoute.getId()).orElseThrow();

        assertThat(updatedRoute.getDestinationAirport().getId()).isEqualTo(newDestination.getId());
        assertThat(updatedRoute.getUpdatedBy()).isEqualTo("test_admin");
        assertThat(updatedRoute.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldDeleteRouteAndPopulateDeletionAuditFields() {
        Airport origin = persistValidAirport("MAD");
        Airport destination = persistValidAirport("JFK");

        Route newRoute = Route.builder()
                .originAirport(origin)
                .destinationAirport(destination)
                .build();

        Route savedRoute = entityManager.persistAndFlush(newRoute);
        entityManager.clear();

        Route retrievedRoute = routeRepository.findById(savedRoute.getId()).orElseThrow();
        retrievedRoute.markAsDeleted("system_test");

        entityManager.flush();
        entityManager.clear();

        Object[] routeResult = (Object[]) entityManager.getEntityManager()
            .createNativeQuery("SELECT deleted_by, deleted_at FROM routes WHERE id = :id")
            .setParameter("id", savedRoute.getId())
            .getSingleResult();
       
        assertThat(routeResult[0]).isEqualTo("system_test");
        assertThat(routeResult[1]).isNotNull();
    }

    @Test
    void shouldExistsByOriginAirportIdAndDestinationAirportIdReturnTrue() {
        Airport origin = persistValidAirport("MAD");
        Airport destination = persistValidAirport("JFK");

        Route newRoute = Route.builder()
                .originAirport(origin)
                .destinationAirport(destination)
                .build();

        entityManager.persistAndFlush(newRoute);
        entityManager.clear();

        boolean exists = routeRepository.existsByOriginAirportIdAndDestinationAirportId(
                origin.getId(), destination.getId());
                
        assertThat(exists).isTrue();
    }
    
    @Test
    void shouldExistsByOriginAirportIdAndDestinationAirportIdReturnFalse() {
        boolean exists = routeRepository.existsByOriginAirportIdAndDestinationAirportId(999L, 888L);
        assertThat(exists).isFalse();
    }

    @Test
    void shouldThrowExceptionWhenOriginAndDestinationAreTheSame() {
        Airport sameAirport = persistValidAirport("LHR");

        Route invalidRoute = Route.builder()
                .originAirport(sameAirport)
                .destinationAirport(sameAirport)
                .build();

        assertThatThrownBy(() -> entityManager.persistAndFlush(invalidRoute))
                .isInstanceOf(Exception.class); 
    }
}