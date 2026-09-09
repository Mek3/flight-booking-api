package com.aerolinea.flight_booking_api.models.enums;

import java.util.EnumSet;
import java.util.Set;

public enum SeatReservationStatus {

    HELD,
    CONFIRMED,
    EXPIRED;

    private static final Set<SeatReservationStatus> OCCUPYING = EnumSet.of(HELD, CONFIRMED);

    public boolean canTransitionTo(SeatReservationStatus target) {
        return switch (this) {
            case HELD -> target == CONFIRMED || target == EXPIRED;
            case CONFIRMED, EXPIRED -> false;
        };
    }

    public boolean occupiesSeat() {
        return OCCUPYING.contains(this);
    }
}