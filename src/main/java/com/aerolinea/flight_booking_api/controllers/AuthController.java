package com.aerolinea.flight_booking_api.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aerolinea.flight_booking_api.dtos.LoginRequest;
import com.aerolinea.flight_booking_api.dtos.RegisterRequest;
import com.aerolinea.flight_booking_api.services.AuthenticationService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;


    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid RegisterRequest registerRequest) {

        return ResponseEntity.status(HttpStatus.CREATED).body(authenticationService.registerUser(registerRequest));
    }


    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody @Valid LoginRequest loginRequest) {
        
        return ResponseEntity.ok().body(authenticationService.loginUser(loginRequest));
    }
    
    
}
