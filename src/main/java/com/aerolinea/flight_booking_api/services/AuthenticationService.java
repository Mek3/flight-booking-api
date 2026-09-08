package com.aerolinea.flight_booking_api.services;

import com.aerolinea.flight_booking_api.dtos.LoginRequest;
import com.aerolinea.flight_booking_api.dtos.RegisterRequest;

public interface AuthenticationService {
    String registerUser(RegisterRequest registerRequest);

    String loginUser(LoginRequest loginRequest);

}
