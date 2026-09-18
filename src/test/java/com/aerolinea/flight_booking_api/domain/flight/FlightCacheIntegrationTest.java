package com.aerolinea.flight_booking_api.domain.flight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import com.aerolinea.flight_booking_api.dtos.flight.FlightSearchResultDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.aerolinea.flight_booking_api.config.AbstractIntegrationTest;
import com.aerolinea.flight_booking_api.dtos.FlightSearchCriteria;
import com.aerolinea.flight_booking_api.dtos.booking.BookingRequest;
import com.aerolinea.flight_booking_api.models.AircraftLayout;
import com.aerolinea.flight_booking_api.models.AircraftModel;
import com.aerolinea.flight_booking_api.models.Airport;
import com.aerolinea.flight_booking_api.models.FlightInstance;
import com.aerolinea.flight_booking_api.models.FlightSchedule;
import com.aerolinea.flight_booking_api.models.User;
import com.aerolinea.flight_booking_api.repositories.AircraftLayoutRepository;
import com.aerolinea.flight_booking_api.repositories.AircraftModelRepository;
import com.aerolinea.flight_booking_api.repositories.AirportRepository;
import com.aerolinea.flight_booking_api.repositories.FlightInstanceRepository;
import com.aerolinea.flight_booking_api.repositories.FlightScheduleRepository;
import com.aerolinea.flight_booking_api.repositories.SeatRepository;
import com.aerolinea.flight_booking_api.repositories.UserRepository;
import com.aerolinea.flight_booking_api.services.BookingService;
import com.aerolinea.flight_booking_api.services.FlightService;
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
    private FlightInstanceRepository flightInstanceRepository;

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

        FlightInstance bookableInstance = persistBookableFlightInstance();

        FlightSearchCriteria criteria = new FlightSearchCriteria("MAD", "JFK", null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        Page<FlightSearchResultDTO> firstSearch = flightService.searchFlights(criteria, pageable);

        assertThat(firstSearch.getContent())
                .as("the materialised flight must be searchable")
                .isNotEmpty();
        assertThat(firstSearch.getContent().get(0).availableSeats())
                .as("availability is derived from the materialised seats")
                .isEqualTo(15);

        verify(flightInstanceRepository, times(1))
                .findAll(org.mockito.ArgumentMatchers.<Specification<FlightInstance>>any(), eq(pageable));

        flightService.searchFlights(criteria, pageable);

        verify(flightInstanceRepository, times(1))
                .findAll(org.mockito.ArgumentMatchers.<Specification<FlightInstance>>any(), eq(pageable));

        Long validSeatId = seatRepository.findAll().stream()
                .filter(seat -> seat.getFlightInstance().getId().equals(bookableInstance.getId()))
                .findFirst()
                .orElseThrow()
                .getId();

        BookingRequest request = new BookingRequest(1, List.of(
                new BookingRequest.ItineraryRequest(List.of(
                        new BookingRequest.FlightSegmentRequest(bookableInstance.getId(), List.of(validSeatId))
                ))
        ));
        bookingService.createBooking(request);

        Page<FlightSearchResultDTO> afterBooking = flightService.searchFlights(criteria, pageable);

        verify(flightInstanceRepository, times(2))
                .findAll(org.mockito.ArgumentMatchers.<Specification<FlightInstance>>any(), eq(pageable));

        assertThat(afterBooking.getContent().get(0).availableSeats())
                .as("the booked seat must no longer count as available")
                .isEqualTo(14);
    }

}