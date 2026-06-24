package com.aerolinea.flight_booking_api.controllers;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.aerolinea.flight_booking_api.dtos.ApiError;
import com.aerolinea.flight_booking_api.exceptions.AppBaseException;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionController {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException exception, WebRequest request){

        String validationException = exception.getBindingResult().getFieldErrors().stream().map(fieldError -> fieldError.getField() 
                                    + ": " + fieldError.getDefaultMessage()).collect(Collectors.joining(", "));

        log.warn("Validation error: {}", validationException);

        ApiError errorApi = new ApiError(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            ErrorCode.VALIDATION_ERROR.getCode(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            validationException,
            request.getDescription(false).replace("uri=", "")
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorApi);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException exception, WebRequest request) {
        log.warn("Database conflict: {}", exception.getMessage());
        ApiError errorApi = new ApiError(
            LocalDateTime.now(),
            HttpStatus.CONFLICT.value(),
            ErrorCode.DATABASE_CONFLICT.getCode(),
            HttpStatus.CONFLICT.getReasonPhrase(),
            "Database integrity violation: The resource conflict suggests it may already exist or violates data constraints.",
            request.getDescription(false).replace("uri=", "")
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorApi);
    }


    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiError> handleAuthorizationDeniedException(
        AuthorizationDeniedException exception, WebRequest request) {    
        log.warn("Access denied: {}", exception.getMessage());

        ApiError apiError = new ApiError(
            LocalDateTime.now(),
            HttpStatus.FORBIDDEN.value(),
            ErrorCode.INSUFFICIENT_PERMISSIONS.getCode(),
            HttpStatus.FORBIDDEN.getReasonPhrase(),
            "Access Denied: You do not have the required roles to perform this action.",
            request.getDescription(false).replace("uri=", "")
        );
    
        return new ResponseEntity<>(apiError, HttpStatus.FORBIDDEN);
    }


    @ExceptionHandler(AppBaseException.class)
    public ResponseEntity<ApiError> handleAppBaseException(AppBaseException appBaseException, WebRequest webRequest){
        log.warn("Business rule violated: {}", appBaseException.getMessage());

        ApiError apiError = new ApiError(
            LocalDateTime.now(),
            appBaseException.getCodeStatusHttp(),
            appBaseException.getErrorCode().getCode(),
            HttpStatus.valueOf(appBaseException.getCodeStatusHttp()).getReasonPhrase(),
            appBaseException.getMessage(),
            webRequest.getDescription(false).replace("uri=", "")
        );

        return ResponseEntity.status(appBaseException.getCodeStatusHttp()).body(apiError);
    }

    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(Exception exception, WebRequest request){
        log.error("Unhandled exception caught in GlobalExceptionHandler", exception);

        ApiError errorApi =  new ApiError(
            LocalDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            ErrorCode.INTERNAL_FATAL_ERROR.getCode(),
            HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
            "An unexpected error occurred. Please contact support.",
            request.getDescription(false).replace("uri=", "")
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorApi);

    }
}
