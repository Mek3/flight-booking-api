package com.aerolinea.flight_booking_api.schedulers;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.aerolinea.flight_booking_api.services.ReservationService;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class ReservationCleanupScheduler {
    private final ReservationService reservationService;

    @Scheduled(fixedDelayString = "${app.scheduling.reservation-cleanup-delay}")
    public void run(){
        reservationService.expirePendingReservations();
    }
}
