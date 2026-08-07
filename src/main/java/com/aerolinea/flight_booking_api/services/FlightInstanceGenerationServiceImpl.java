package com.aerolinea.flight_booking_api.services;

import com.aerolinea.flight_booking_api.models.FlightSchedule;
import com.aerolinea.flight_booking_api.repositories.FlightScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FlightInstanceGenerationServiceImpl implements FlightInstanceGenerationService {

    private final FlightScheduleRepository flightScheduleRepository;
    private final FlightInstanceService flightInstanceService;

    @Override
    public void generateUpcomingFlightInstances() {
        LocalDate targetDate = LocalDate.now().plusDays(30);
        DayOfWeek day = targetDate.getDayOfWeek();

        Integer daysOfWeekMask = 1 << (day.getValue() - 1);

        Pageable pageable = PageRequest.of(0, 500);
        Page<FlightSchedule> page;

        do {
            page = flightScheduleRepository.findAllByDaysOfWeekMask(pageable, daysOfWeekMask);

            for (FlightSchedule schedule : page.getContent()) {
                try {
                    flightInstanceService.generateFlightInstance(schedule, targetDate);

                } catch (DataIntegrityViolationException e) {
                    log.warn("Flight instance already exists for schedule ID {} on date {}. Skipping...", schedule.getId(), targetDate, e);

                } catch (Exception e) {
                    log.error("Unexpected error generating flight instance for schedule ID {}: {}", schedule.getId(), e.getMessage());
                }
            }

            pageable = page.nextPageable();

        } while (page.hasNext());
    }

}