package com.aerolinea.flight_booking_api.controllers;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.aerolinea.flight_booking_api.dtos.ApiError;
import com.aerolinea.flight_booking_api.exceptions.AppBaseException;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionController {

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException ex, WebRequest webRequest) {
        String path = webRequest.getDescription(false).replace("uri=", "");
    
        log.warn("Method not allowed: {} for URI: {}", ex.getMethod(), path);
        
        ApiError apiError = new ApiError(
                LocalDateTime.now(),
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                ErrorCode.METHOD_NOT_ALLOWED.getCode(),
                "Method Not Allowed",
                ex.getMessage(),
                path
        );
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(apiError);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFoundException(NoResourceFoundException ex, WebRequest webRequest) {
        String path = webRequest.getDescription(false).replace("uri=", "");
    
        log.warn("Enpoint not found: {}", path);
        
        ApiError apiError = new ApiError(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                ErrorCode.ENDPOINT_NOT_FOUND.getCode(),
                "Not Found",
                ex.getMessage(),
                path
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentialsException(BadCredentialsException exception, WebRequest webRequest) {
        log.warn("Failed login attempt: {}", exception.getMessage());

        ApiError apiError = new ApiError(
            LocalDateTime.now(),
            HttpStatus.UNAUTHORIZED.value(), 
            ErrorCode.INVALID_CREDENTIALS.getCode(), 
            HttpStatus.UNAUTHORIZED.getReasonPhrase(),
            "Invalid username or password.", 
            webRequest.getDescription(false).replace("uri=", "")
        );
    
        return new ResponseEntity<>(apiError, HttpStatus.UNAUTHORIZED);
    }

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


    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, CannotAcquireLockException.class})
    public ResponseEntity<ApiError> handleConcurrencyFailure(Exception ex, WebRequest webRequest) {
        String path = webRequest.getDescription(false).replace("uri=", "");
        
        log.warn("Optimistic locking or concurrency conflict detected: {} for URI: {}", ex.getMessage(), path);

        ApiError apiError = new ApiError(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                ErrorCode.CONCURRENCY_CONFLICT.getCode(), 
                HttpStatus.CONFLICT.getReasonPhrase(),
                "The resource was modified by another transaction. Please refresh and try again.",
                path
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
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
