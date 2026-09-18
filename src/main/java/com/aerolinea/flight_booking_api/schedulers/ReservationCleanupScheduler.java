package com.aerolinea.flight_booking_api.schedulers;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.aerolinea.flight_booking_api.services.ReservationService;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class ReservationCleanupScheduler {
    private final ReservationService reservationService;

    @SchedulerLock(name = "ReservationCleanupScheduler_run",
            lockAtLeastFor = "${app.scheduling.reservation-cleanup.lock-at-least-for:5s}",
            lockAtMostFor = "${app.scheduling.reservation-cleanup.lock-at-most-for:50s}")
    @Scheduled(fixedDelayString = "${app.scheduling.reservation-cleanup.delay}")
    public void run(){
        reservationService.expirePendingReservations();
    }
}
