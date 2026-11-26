package com.aerolinea.flight_booking_api.models.enums;

public enum ReservationStatus {

    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED,
    EXPIRED;

    public boolean canTransitionTo(ReservationStatus target) {
        return switch (this) {
            case PENDING -> target == CONFIRMED || target == CANCELLED || target == EXPIRED;
            case CONFIRMED -> target == CANCELLED || target == COMPLETED;
            case CANCELLED, COMPLETED, EXPIRED -> false;
        };
    }
}
