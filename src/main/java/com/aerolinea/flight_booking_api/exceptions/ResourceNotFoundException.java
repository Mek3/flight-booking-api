package com.aerolinea.flight_booking_api.exceptions;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends AppBaseException {

    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.NOT_FOUND.value());
    }

}
