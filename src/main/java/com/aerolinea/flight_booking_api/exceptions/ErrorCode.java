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
    ACCESS_DENIED(3006L, "Access Denied: You do not have the required roles to perform this action."),

    // Airport Domain (4000 - 4999)
    AIRPORT_NOT_FOUND(4001L, "Airport not found with ID: %s"),

    // Aircraft Model Domain (5000 - 5999)
    AIRCRAFT_MODEL_NOT_FOUND(5001L, "Aircraft model not found with ID: %s"),
    AIRCRAFT_MODEL_IN_USE(5002L, "Aircraft model with ID: %s cannot be deleted because it is assigned to existing flights"),
    AIRCRAFT_MODEL_ALREADY_EXISTS(5003L, "An aircraft model with the specified manufacturer '%s' and model name '%s' already exists"),

    // Aircraft Domain (6000 - 6999)
    AIRCRAFT_NOT_FOUND(6001L, "Aircraft not found with ID: %s"),
    AIRCRAFT_ALREADY_EXISTS(6002L, "An aircraft with the registration number '%s' already exists"),

    // Route Domain (7000 - 7999)
    ROUTE_NOT_FOUND(7001L, "Route not found with ID: %s"),
    ROUTE_ALREADY_EXISTS(7002L, "A route between origin airport ID %s and destination airport ID %s already exists"),
    ROUTE_SAME_ORIGIN_DESTINATION(7003L, "Origin airport and destination airport cannot be the same (Airport ID: %s)"),

    // Flight Schedule Domain (8000 - 8099)
    FLIGHT_SCHEDULE_NOT_FOUND(8001L, "Flight schedule not found with ID: %s"),
    FLIGHT_SCHEDULE_ALREADY_EXISTS(8002L, "A flight schedule with flight number '%s' already exists"),

    // Flight Instance Domain (8100 - 8199)
    FLIGHT_INSTANCE_NOT_FOUND(8101L, "Flight instance not found with ID: %s"),
    FLIGHT_INSTANCE_ALREADY_EXISTS(8102L, "Flight instance already exists for schedule ID %s on date %s"),

    // Aircraft Layout Domain (8200 - 8299)
    AIRCRAFT_LAYOUT_NOT_FOUND(8201L, "Aircraft layout not found with ID: %s"),
    AIRCRAFT_LAYOUT_INVALID_GEOMETRY(8202L, "Invalid seat geometry for layout ID %s: total_rows and seat_letters must be defined correctly"),

    // Seat Domain (8300 - 8399)
    SEAT_NOT_FOUND(8301L, "Seat not found with ID: %s"),
    SEAT_BULK_GENERATION_FAILED(8302L, "Failed to materialize bulk seats for flight instance ID: %s"),
    SEAT_ALREADY_BOOKED(8303L, "Seat %s is already booked for flight instance ID: %s"),

    // System & Framework (9000 - 9999)
    VALIDATION_ERROR(9001L, "Validation error: %s"),
    DATABASE_CONFLICT(9002L, "Database conflict detected: The resource may already exist or violates data constraints"),
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