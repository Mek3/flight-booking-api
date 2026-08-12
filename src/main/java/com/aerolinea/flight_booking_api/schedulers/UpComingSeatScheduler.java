package com.aerolinea.flight_booking_api.schedulers;


import com.aerolinea.flight_booking_api.services.GenerationSeatService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpComingSeatScheduler {

    private final GenerationSeatService generationSeatService;

    @Scheduled(cron = "${app.scheduling.seat-generation.cron}")
    @SchedulerLock(name = "UpComingSeatScheduler_run",
            lockAtLeastFor = "${app.scheduling.seat-generation.lock-at-least-for:5m}",
            lockAtMostFor = "${app.scheduling.seat-generation.lock-at-most-for:15m}")
    public void run() {
        generationSeatService.generateUpcomingSeat();
    }
}
