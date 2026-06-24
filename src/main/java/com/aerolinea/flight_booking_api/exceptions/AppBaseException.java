package com.aerolinea.flight_booking_api.exceptions;

import lombok.Getter;

@Getter
public abstract class AppBaseException extends RuntimeException {

    protected ErrorCode errorCode;
    protected String message;
    protected int codeStatusHttp;
    
    protected AppBaseException(ErrorCode errorCode, String message, int codeStatusHttp){
        super(message);
        this.errorCode = errorCode;
        this.message = message;
        this.codeStatusHttp = codeStatusHttp;
    }

}
