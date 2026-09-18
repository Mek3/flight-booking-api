package com.aerolinea.flight_booking_api.domain.flight;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.aerolinea.flight_booking_api.services.FlightService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import com.aerolinea.flight_booking_api.config.AbstractIntegrationTest;
import com.aerolinea.flight_booking_api.dtos.FlightSearchCriteria;
import com.aerolinea.flight_booking_api.dtos.flight.FlightSearchResultDTO;
import com.aerolinea.flight_booking_api.models.AircraftLayout;
import com.aerolinea.flight_booking_api.models.AircraftModel;
import com.aerolinea.flight_booking_api.models.Airport;
import com.aerolinea.flight_booking_api.models.FlightInstance;
import com.aerolinea.flight_booking_api.models.FlightSchedule;
import com.aerolinea.flight_booking_api.repositories.AircraftLayoutRepository;
import com.aerolinea.flight_booking_api.repositories.AircraftModelRepository;
import com.aerolinea.flight_booking_api.repositories.AirportRepository;
import com.aerolinea.flight_booking_api.repositories.FlightInstanceRepository;
import com.aerolinea.flight_booking_api.repositories.FlightScheduleRepository;
import com.aerolinea.flight_booking_api.repositories.SeatRepository;
import com.aerolinea.flight_booking_api.utils.factories.AircraftLayoutFactory;
import com.aerolinea.flight_booking_api.utils.factories.AircraftModelFactory;
import com.aerolinea.flight_booking_api.utils.factories.AirportFactory;
import com.aerolinea.flight_booking_api.utils.factories.FlightInstanceFactory;
import com.aerolinea.flight_booking_api.utils.factories.FlightScheduleFactory;

@SpringBootTest
@Transactional
public class FlightSearchIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private FlightService flightSearchService;

    @Autowired
    private AirportRepository airportRepository;

    @Autowired
    private AircraftModelRepository aircraftModelRepository;

    @Autowired
    private AircraftLayoutRepository aircraftLayoutRepository;

    @Autowired
    private FlightScheduleRepository flightScheduleRepository;

    @Autowired
    private FlightInstanceRepository flightInstanceRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private CacheManager cacheManager;

    private final Pageable pageable = PageRequest.of(0, 10);

    @BeforeEach
    void setUp() {
        cacheManager.getCache("flightSearchCache").clear();

        Airport mad = airportRepository.save(AirportFactory.validAirportBuilder("MAD").build());
        Airport jfk = airportRepository.save(AirportFactory.validAirportBuilder("JFK").build());
        Airport bcn = airportRepository.save(AirportFactory.validAirportBuilder("BCN").build());
        Airport cdg = airportRepository.save(AirportFactory.validAirportBuilder("CDG").build());

        AircraftModel model = aircraftModelRepository.save(AircraftModelFactory.validModelBuilder().build());
        AircraftLayout layout = aircraftLayoutRepository.save(AircraftLayoutFactory.validLayoutBuilder(model).build());

        FlightSchedule schedule1 = flightScheduleRepository.save(FlightScheduleFactory.validScheduleBuilder(mad, jfk, layout)
                .flightNumber("IBE-001")
                .departureTime(LocalTime.of(10, 0))
                .arrivalTime(LocalTime.of(18, 0))
                .basePrice(new BigDecimal("400.00"))
                .build());
        FlightInstance flight1 = flightInstanceRepository.save(FlightInstanceFactory.validInstanceBuilder(schedule1)
                .departureDate(LocalDate.of(2026, 7, 15))
                .build());
        seatRepository.batchInsertSeats(flight1.getId(), 100, "A"); // 100 asientos

        FlightSchedule schedule2 = flightScheduleRepository.save(FlightScheduleFactory.validScheduleBuilder(mad, jfk, layout)
                .flightNumber("IBE-002")
                .departureTime(LocalTime.of(22, 30))
                .arrivalTime(LocalTime.of(6, 30))
                .arrivalDayOffset(1)
                .basePrice(new BigDecimal("250.00"))
                .build());
        FlightInstance flight2 = flightInstanceRepository.save(FlightInstanceFactory.validInstanceBuilder(schedule2)
                .departureDate(LocalDate.of(2026, 7, 15))
                .build());
        seatRepository.batchInsertSeats(flight2.getId(), 5, "A"); // 5 asientos

        FlightSchedule schedule3 = flightScheduleRepository.save(FlightScheduleFactory.validScheduleBuilder(bcn, cdg, layout)
                .flightNumber("AEA-003")
                .departureTime(LocalTime.of(12, 0))
                .arrivalTime(LocalTime.of(14, 0))
                .basePrice(new BigDecimal("150.00"))
                .build());
        FlightInstance flight3 = flightInstanceRepository.save(FlightInstanceFactory.validInstanceBuilder(schedule3)
                .departureDate(LocalDate.of(2026, 7, 16))
                .build());
        seatRepository.batchInsertSeats(flight3.getId(), 50, "A"); // 50 asientos
    }

    @Test
    void givenFlightsInDb_whenSearchByRoute_thenReturnMatchingFlights() {
        FlightSearchCriteria criteria = new FlightSearchCriteria("MAD", "JFK", null, null, null, null);

        Page<FlightSearchResultDTO> result = flightSearchService.searchFlights(criteria, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(FlightSearchResultDTO::flightNumber)
                .containsExactlyInAnyOrder("IBE-001", "IBE-002");
    }

    @Test
    void givenFlightsInDb_whenSearchByPriceRange_thenReturnFlightsWithinRange() {
        FlightSearchCriteria criteria = new FlightSearchCriteria(null, null, new BigDecimal("100.00"), new BigDecimal("280.00"), null, null);

        Page<FlightSearchResultDTO> result = flightSearchService.searchFlights(criteria, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(FlightSearchResultDTO::flightNumber)
                .containsExactlyInAnyOrder("IBE-002", "AEA-003");
    }

    @Test
    void givenFlightsInDb_whenSearchByDate_thenReturnFlightsWithinThatDayWindow() {
        FlightSearchCriteria criteria = new FlightSearchCriteria(null, null, null, null, null, LocalDate.of(2026, 7, 15));

        Page<FlightSearchResultDTO> result = flightSearchService.searchFlights(criteria, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(FlightSearchResultDTO::flightNumber)
                .containsExactlyInAnyOrder("IBE-001", "IBE-002");
    }

    @Test
    void givenFlightsInDb_whenSearchWithCombinedCriteria_thenReturnExactMatch() {
        FlightSearchCriteria criteria = new FlightSearchCriteria("MAD", "JFK", new BigDecimal("200.00"), new BigDecimal("300.00"), 2, LocalDate.of(2026, 7, 15));

        Page<FlightSearchResultDTO> result = flightSearchService.searchFlights(criteria, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).flightNumber()).isEqualTo("IBE-002");
    }

    @Test
    void givenFlightsInDb_whenSearchWithEmptyCriteria_thenReturnAllFlights() {
        FlightSearchCriteria criteria = new FlightSearchCriteria(null, null, null, null, null, null);

        Page<FlightSearchResultDTO> result = flightSearchService.searchFlights(criteria, pageable);

        assertThat(result.getTotalElements()).isEqualTo(3L);
    }
}