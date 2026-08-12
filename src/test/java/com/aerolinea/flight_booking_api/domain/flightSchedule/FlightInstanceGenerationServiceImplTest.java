package com.aerolinea.flight_booking_api.domain.flightSchedule;

import com.aerolinea.flight_booking_api.models.AircraftLayout;
import com.aerolinea.flight_booking_api.models.AircraftModel;
import com.aerolinea.flight_booking_api.models.Airport;
import com.aerolinea.flight_booking_api.models.FlightSchedule;
import com.aerolinea.flight_booking_api.repositories.FlightScheduleRepository;
import com.aerolinea.flight_booking_api.services.FlightInstanceGenerationServiceImpl;
import com.aerolinea.flight_booking_api.services.FlightInstanceService;
import com.aerolinea.flight_booking_api.utils.factories.AircraftLayoutFactory;
import com.aerolinea.flight_booking_api.utils.factories.AircraftModelFactory;
import com.aerolinea.flight_booking_api.utils.factories.AirportFactory;
import com.aerolinea.flight_booking_api.utils.factories.FlightScheduleFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightInstanceGenerationServiceImplTest {

    @Mock
    private FlightScheduleRepository flightScheduleRepository;

    @Mock
    private FlightInstanceService flightInstanceService;

    @InjectMocks
    private FlightInstanceGenerationServiceImpl generationService;

    @Test
    void generateUpcomingFlightInstances_shouldProcessAllSchedulesAndSurviveExceptions() {
        Airport departure = AirportFactory.validAirportBuilder("MAD").build();
        Airport arrival = AirportFactory.validAirportBuilder("JFK").build();
        AircraftModel aircrafmodel = AircraftModelFactory.validModelBuilder().build();
        AircraftLayout aircraftLayout = AircraftLayoutFactory.validLayoutBuilder(aircrafmodel).build();

        FlightSchedule successfulSchedule1 = FlightScheduleFactory.validScheduleBuilder(departure, arrival, aircraftLayout)
                .flightNumber("IBE001").build();

        FlightSchedule duplicateSchedule = FlightScheduleFactory.validScheduleBuilder(departure, arrival, aircraftLayout)
                .flightNumber("IBE002").build();

        FlightSchedule successfulSchedule2 = FlightScheduleFactory.validScheduleBuilder(departure, arrival, aircraftLayout)
                .flightNumber("IBE003").build();

        PageRequest pageable = PageRequest.of(0, 500);
        when(flightScheduleRepository.findAllByDaysOfWeekMask(any(Pageable.class), anyInt()))
                .thenReturn(new PageImpl<>(List.of(successfulSchedule1, duplicateSchedule, successfulSchedule2), pageable, 3));

        doNothing()
                .when(flightInstanceService).generateFlightInstance(eq(successfulSchedule1), any(LocalDate.class));

        doThrow(new DataIntegrityViolationException("Duplicate key error simulated"))
                .when(flightInstanceService).generateFlightInstance(eq(duplicateSchedule), any(LocalDate.class));

        doNothing()
                .when(flightInstanceService).generateFlightInstance(eq(successfulSchedule2), any(LocalDate.class));

        generationService.generateUpcomingFlightInstances();

        verify(flightScheduleRepository, atLeastOnce()).findAllByDaysOfWeekMask(any(Pageable.class), anyInt());

        verify(flightInstanceService, times(1)).generateFlightInstance(eq(successfulSchedule1), any(LocalDate.class));
        verify(flightInstanceService, times(1)).generateFlightInstance(eq(duplicateSchedule), any(LocalDate.class));
        verify(flightInstanceService, times(1)).generateFlightInstance(eq(successfulSchedule2), any(LocalDate.class));
    }
}