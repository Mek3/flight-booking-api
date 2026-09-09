package com.aerolinea.flight_booking_api.domain.flight;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.aerolinea.flight_booking_api.dtos.booking.BookingRequest;
import com.aerolinea.flight_booking_api.services.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.aerolinea.flight_booking_api.config.AbstractIntegrationTest;
import com.aerolinea.flight_booking_api.dtos.FlightSearchCriteria;
import com.aerolinea.flight_booking_api.dtos.ReservationRequest;
import com.aerolinea.flight_booking_api.models.AircraftLayout;
import com.aerolinea.flight_booking_api.models.AircraftModel;
import com.aerolinea.flight_booking_api.models.Airport;
import com.aerolinea.flight_booking_api.models.Flight;
import com.aerolinea.flight_booking_api.models.FlightInstance;
import com.aerolinea.flight_booking_api.models.FlightSchedule;
import com.aerolinea.flight_booking_api.models.User;
import com.aerolinea.flight_booking_api.repositories.AircraftLayoutRepository;
import com.aerolinea.flight_booking_api.repositories.AircraftModelRepository;
import com.aerolinea.flight_booking_api.repositories.AirportRepository;
import com.aerolinea.flight_booking_api.repositories.FlightInstanceRepository;
import com.aerolinea.flight_booking_api.repositories.FlightRepository;
import com.aerolinea.flight_booking_api.repositories.FlightScheduleRepository;
import com.aerolinea.flight_booking_api.repositories.SeatRepository;
import com.aerolinea.flight_booking_api.repositories.UserRepository;
import com.aerolinea.flight_booking_api.services.FlightService;
import com.aerolinea.flight_booking_api.services.ReservationService;
import com.aerolinea.flight_booking_api.utils.factories.AircraftLayoutFactory;
import com.aerolinea.flight_booking_api.utils.factories.AircraftModelFactory;
import com.aerolinea.flight_booking_api.utils.factories.AirportFactory;
import com.aerolinea.flight_booking_api.utils.factories.FlightInstanceFactory;
import com.aerolinea.flight_booking_api.utils.factories.FlightScheduleFactory;

@SpringBootTest
public class FlightCacheIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private FlightService flightService;

    @Autowired
    private BookingService bookingService;

    @MockitoSpyBean
    private FlightRepository flightRepository;

    @Autowired
    private UserRepository userRepository;

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

    @BeforeEach
    void setUp() {
        cacheManager.getCache("flightSearchCache").clear();
    }

    private FlightInstance persistBookableFlightInstance() {
        Airport departure = airportRepository.save(AirportFactory.validAirportBuilder("MAD").build());
        Airport arrival = airportRepository.save(AirportFactory.validAirportBuilder("JFK").build());
        AircraftModel model = aircraftModelRepository.save(AircraftModelFactory.validModelBuilder().build());
        AircraftLayout layout = aircraftLayoutRepository.save(AircraftLayoutFactory.validLayoutBuilder(model).build());

        FlightSchedule schedule = flightScheduleRepository.save(
                FlightScheduleFactory.validScheduleBuilder(departure, arrival, layout)
                        .flightNumber("CACHE-999")
                        .build());

        FlightInstance flightInstance = flightInstanceRepository.save(
                FlightInstanceFactory.validInstanceBuilder(schedule).build());

        seatRepository.batchInsertSeats(flightInstance.getId(), 5, "ABC");

        return flightInstance;
    }

    @Test
    @DisplayName("Should use cache for repeated searches and evict it when a reservation is created")
    @WithMockUser(username = "UserCache", roles = "USER")
    void shouldCacheFlightSearchResultsAndEvictOnReservationCreation() {

        User testUser = User.builder()
                .username("UserCache")
                .email("userCache@cachetest.com")
                .password("encoded_pass")
                .name("UserCache")
                .surname("UserCache")
                .build();
        userRepository.save(testUser);

        Flight testFlight = Flight.builder()
                .flightNumber("CACHE-999")
                .departure("Londres")
                .departureTime(LocalDateTime.now().plusDays(5))
                .destination("Budapest")
                .destinationTime(LocalDateTime.now().plusDays(5).plusHours(12))
                .availableSeats(10)
                .price(new BigDecimal("500.00"))
                .build();

        flightRepository.save(testFlight);

        FlightInstance bookableInstance = persistBookableFlightInstance();

        FlightSearchCriteria criteria = new FlightSearchCriteria("Londres", "Budapest", null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        flightService.searchFlights(criteria, pageable);
        verify(flightRepository, times(1)).findAll(org.mockito.ArgumentMatchers.<Specification<Flight>>any(), eq(pageable));

        flightService.searchFlights(criteria, pageable);
        verify(flightRepository, times(1)).findAll(org.mockito.ArgumentMatchers.<Specification<Flight>>any(), eq(pageable));

        BookingRequest request = new BookingRequest(1, List.of(
                new BookingRequest.ItineraryRequest(List.of(bookableInstance.getId()))));
        bookingService.createBooking(request);

        flightService.searchFlights(criteria, pageable);
        verify(flightRepository, times(2)).findAll(org.mockito.ArgumentMatchers.<Specification<Flight>>any(), eq(pageable));
    }

}