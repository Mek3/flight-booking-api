package com.aerolinea.flight_booking_api.domain.flight;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

import com.aerolinea.flight_booking_api.config.AbstractIntegrationTest;
import com.aerolinea.flight_booking_api.dtos.FlightSearchCriteria;
import com.aerolinea.flight_booking_api.dtos.ReservationRequest;
import com.aerolinea.flight_booking_api.models.Flight;
import com.aerolinea.flight_booking_api.repositories.FlightRepository;
import com.aerolinea.flight_booking_api.services.FlightService;
import com.aerolinea.flight_booking_api.services.ReservationService;

@Transactional
public class FlightCacheIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private FlightService flightService;

    @Autowired
    private ReservationService reservationService;

    @MockitoSpyBean
    private FlightRepository flightRepository;

    @Autowired
    private CacheManager cacheManager;


    @BeforeEach
    void setUp() {
        cacheManager.getCache("flightSearchCache").clear();
    }

    @Test
    @DisplayName("Should use cache for repeated searches and evict it when a reservation is created")
    @WithMockUser(username = "pacog", roles = "USER")
    void shouldCacheFlightSearchResultsAndEvictOnReservationCreation() {
                    
       FlightSearchCriteria criteria = new FlightSearchCriteria(
            "Madrid",     
            "Tokyo",      
            null,        
            null,         
            null,        
            null          
        );

        Pageable pageable = PageRequest.of(0, 10);

        flightService.searchFlights(criteria, pageable);
        verify(flightRepository, times(1)).findAll(org.mockito.ArgumentMatchers.<Specification<Flight>>any(), eq(pageable));

        flightService.searchFlights(criteria, pageable);
        verify(flightRepository, times(1)).findAll(org.mockito.ArgumentMatchers.<Specification<Flight>>any(), eq(pageable));

        ReservationRequest request = new ReservationRequest(1L, 1); 
        reservationService.createReservation(request);

        flightService.searchFlights(criteria, pageable);
        verify(flightRepository, times(2)).findAll(org.mockito.ArgumentMatchers.<Specification<Flight>>any(), eq(pageable));

    }

}
