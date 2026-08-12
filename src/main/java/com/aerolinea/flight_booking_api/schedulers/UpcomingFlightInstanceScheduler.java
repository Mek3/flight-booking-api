package com.aerolinea.flight_booking_api.schedulers;

import com.aerolinea.flight_booking_api.services.FlightInstanceGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpcomingFlightInstanceScheduler {

    private final FlightInstanceGenerationService generationService;

    @Scheduled(cron = "${app.scheduling.flight-generation.cron}")
    @SchedulerLock(name = "UpcomingFlightsScheduler_run",
            lockAtLeastFor = "${app.scheduling.flight-generation.lock-at-least-for:5m}",
            lockAtMostFor = "${app.scheduling.flight-generation.lock-at-most-for:15m}")
    public void run() {
        generationService.generateUpcomingFlightInstances();
    }
}
