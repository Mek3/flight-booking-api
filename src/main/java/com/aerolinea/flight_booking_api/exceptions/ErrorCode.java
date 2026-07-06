package com.aerolinea.flight_booking_api.exceptions;

import lombok.Getter;

@Getter
public enum ErrorCode {
   // Flight Domain (1000 - 1999)
    FLIGHT_NOT_FOUND(1001L, "Flight not found with ID: %s"),
    FLIGHT_DEPARTURE_PASSED(1002L, "Flight departure time has already passed for flight ID: %s"),
    NOT_ENOUGH_SEATS(1003L, "Not enough seats available for flight ID: %s"),

    // Reservation Domain (2000 - 2999)
    RESERVATION_NOT_FOUND(2001L, "Reservation not found with ID: %s"),
    RESERVATION_ALREADY_CANCELLED(2002L, "Reservation with ID: %s is already cancelled"),
    RESERVATION_ALREADY_CONFIRMED(2003L, "Reservation with ID: %s is already confirmed"),
    RESERVATION_ALREADY_EXPIRED(2004L, "Reservation with ID: %s is already expired"),
    CANCELLATION_TIME_EXPIRED(2005L, "Cancellations must be made at least 24 hours in advance. Reservation ID: %s"),

    // User & Security Domain (3000 - 3999)
    USER_NOT_FOUND(3001L, "User not found with username: %s"),
    USER_ALREADY_EXISTS(3002L, "User already exists with username: %s"),
    INVALID_CREDENTIALS(3003L, "Invalid username or password"),             
    INVALID_OR_MISSING_TOKEN(3004L, "Authentication token is missing, invalid, or expired"),       
    INSUFFICIENT_PERMISSIONS(3005L, "User %s lacks permissions to access or modify this resource"),

    // System & Framework (9000 - 9999)
    VALIDATION_ERROR(9001L, "Validation error: %s"),
    DATABASE_CONFLICT(9002L, "Database conflict detected: %s"),
    METHOD_NOT_ALLOWED(9003L, "HTTP method %s is not supported for this endpoint"),
    ENDPOINT_NOT_FOUND(9004L, "The requested endpoint %s does not exist"),
    CONCURRENCY_CONFLICT(9005L, "Concurrency conflict: The resource was modified by another transaction"),


    INTERNAL_FATAL_ERROR(9999L, "An unexpected internal error occurred. Please contact support.");

    private final Long code;
    private final String message;

    ErrorCode(Long code, String message) {
        this.code = code;
        this.message = message;
    }
}