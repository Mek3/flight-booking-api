package com.aerolinea.flight_booking_api.exceptions;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // Flight Domain (1000 - 1999)
    FLIGHT_NOT_FOUND(1001L),
    FLIGHT_DEPARTURE_PASSED(1002L),
    NOT_ENOUGH_SEATS(1003L),

    // Reservation Domain (2000 - 2999)
    RESERVATION_NOT_FOUND(2001L),
    RESERVATION_ALREADY_CANCELLED(2002L),
    CANCELLATION_TIME_EXPIRED(2003L),

    // User & Security Domain (3000 - 3999)
    USER_NOT_FOUND(3001L),
    USER_ALREADY_EXISTS(3002L),
    INVALID_CREDENTIALS(3003L),             
    INVALID_OR_MISSING_TOKEN(3004L),       
    INSUFFICIENT_PERMISSIONS(3005L),

    // System & Framework (9000 - 9999)
    VALIDATION_ERROR(9001L),
    DATABASE_CONFLICT(9002L),
    INTERNAL_FATAL_ERROR(9999L);

    private final Long code;

    ErrorCode(Long code) {
        this.code = code;
    }
}