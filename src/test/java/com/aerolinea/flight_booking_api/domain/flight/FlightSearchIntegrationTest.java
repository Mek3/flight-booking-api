package com.aerolinea.flight_booking_api.domain.flight;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import com.aerolinea.flight_booking_api.config.AbstractIntegrationTest;
import com.aerolinea.flight_booking_api.dtos.FlightDTO;
import com.aerolinea.flight_booking_api.dtos.FlightSearchCriteria;
import com.aerolinea.flight_booking_api.models.Flight;
import com.aerolinea.flight_booking_api.repositories.FlightRepository;
import com.aerolinea.flight_booking_api.services.FlightService;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
public class FlightSearchIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private FlightService flightService;

    @Autowired
    private FlightRepository flightRepository;

    private final Pageable pageable = PageRequest.of(0, 10);

    @BeforeEach
    void setUp() {
        Flight flight1 = Flight.builder()
                .flightNumber("IBE-001")
                .departure("MAD")
                .departureTime(LocalDateTime.of(2026, 7, 15, 10, 0))
                .destination("JFK")
                .destinationTime(LocalDateTime.of(2026, 7, 15, 18, 0))
                .availableSeats(100)
                .price(new BigDecimal("400.00"))
                .build();

        Flight flight2 = Flight.builder()
                .flightNumber("IBE-002")
                .departure("MAD")
                .departureTime(LocalDateTime.of(2026, 7, 15, 22, 30))
                .destination("JFK")
                .destinationTime(LocalDateTime.of(2026, 7, 16, 6, 30))
                .availableSeats(5)
                .price(new BigDecimal("250.00"))
                .build();

        Flight flight3 = Flight.builder()
                .flightNumber("AEA-003")
                .departure("BCN")
                .departureTime(LocalDateTime.of(2026, 7, 16, 12, 0))
                .destination("CDG")
                .destinationTime(LocalDateTime.of(2026, 7, 16, 14, 0))
                .availableSeats(50)
                .price(new BigDecimal("150.00"))
                .build();

        flightRepository.saveAndFlush(flight1);
        flightRepository.saveAndFlush(flight2);
        flightRepository.saveAndFlush(flight3);
    }



    @Test
    void givenFlightsInDb_whenSearchByRoute_thenReturnMatchingFlights() {
        FlightSearchCriteria criteria = new FlightSearchCriteria("MAD", "JFK", null, null, null, null);

        Page<FlightDTO> result = flightService.searchFlights(criteria, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(FlightDTO::getFlightNumber).containsExactlyInAnyOrder("IBE-001", "IBE-002");
    }

    @Test
    void givenFlightsInDb_whenSearchByPriceRange_thenReturnFlightsWithinRange() {
        FlightSearchCriteria criteria = new FlightSearchCriteria(null, null, new BigDecimal("100.00"), new BigDecimal("280.00"), null, null);

        Page<FlightDTO> result = flightService.searchFlights(criteria, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(FlightDTO::getFlightNumber).containsExactlyInAnyOrder("IBE-002", "AEA-003");
    }

    @Test
    void givenFlightsInDb_whenSearchByDate_thenReturnFlightsWithinThatDayWindow() {
        FlightSearchCriteria criteria = new FlightSearchCriteria(null, null, null, null, null, LocalDate.of(2026, 7, 15));

        Page<FlightDTO> result = flightService.searchFlights(criteria, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(FlightDTO::getFlightNumber).containsExactlyInAnyOrder("IBE-001", "IBE-002");
    }

    @Test
    void givenFlightsInDb_whenSearchWithCombinedCriteria_thenReturnExactMatch() {
        FlightSearchCriteria criteria = new FlightSearchCriteria("MAD", "JFK", new BigDecimal("200.00"), new BigDecimal("300.00"), 2, LocalDate.of(2026, 7, 15));

        Page<FlightDTO> result = flightService.searchFlights(criteria, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFlightNumber()).isEqualTo("IBE-002");
    }

    @Test
    void givenFlightsInDb_whenSearchWithEmptyCriteria_thenReturnAllFlights() {
        FlightSearchCriteria criteria = new FlightSearchCriteria(null, null, null, null, null, null);

        Page<FlightDTO> result = flightService.searchFlights(criteria, pageable);

        assertThat(result.getTotalElements()).isEqualTo(4);
    }
}