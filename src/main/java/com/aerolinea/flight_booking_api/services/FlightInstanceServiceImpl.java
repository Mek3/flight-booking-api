package com.aerolinea.flight_booking_api.services;

import com.aerolinea.flight_booking_api.models.FlightInstance;
import com.aerolinea.flight_booking_api.models.FlightSchedule;
import com.aerolinea.flight_booking_api.models.enums.FlightStatus;
import com.aerolinea.flight_booking_api.repositories.FlightInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;


@Service
@Slf4j
@RequiredArgsConstructor
public class FlightInstanceServiceImpl implements FlightInstanceService {

    private final FlightInstanceRepository flightInstanceRepository;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void generateFlightInstance(FlightSchedule schedule, LocalDate departureDate) {

        FlightInstance instance = FlightInstance.builder()
                .flightSchedule(schedule)
                .departureDate(departureDate)
                .status(FlightStatus.SCHEDULED)
                .build();

        flightInstanceRepository.save(instance);

        log.debug("Successfully generated flight instance for schedule {} on date {}", schedule.getId(), departureDate);
    }

}
