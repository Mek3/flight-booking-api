package com.aerolinea.flight_booking_api.exceptions;

import org.springframework.http.HttpStatus;

public class BusinessRuleViolationException extends AppBaseException{

    public BusinessRuleViolationException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.CONFLICT.value());
    }

}
