package com.aerolinea.flight_booking_api.services;


import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.aerolinea.flight_booking_api.dtos.LoginRequest;
import com.aerolinea.flight_booking_api.dtos.RegisterRequest;
import com.aerolinea.flight_booking_api.mappers.UserMapper;
import com.aerolinea.flight_booking_api.models.Role;
import com.aerolinea.flight_booking_api.models.User;
import com.aerolinea.flight_booking_api.models.UserRoleAssignment;
import com.aerolinea.flight_booking_api.repositories.RoleRepository;
import com.aerolinea.flight_booking_api.repositories.UserRepository;
import com.aerolinea.flight_booking_api.repositories.UserRoleAssignmentRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService{

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;


    @Override
    public String registerUser(RegisterRequest registerRequest) {
        if(userRepository.existsByUsername(registerRequest.username())) {
            throw new ResponseStatusException (HttpStatus.CONFLICT, "User "+ registerRequest.username() + " already exists");
        }

        User user = userMapper.toEntity(registerRequest);
        String encodePassword = passwordEncoder.encode(registerRequest.password());
        user.setPassword(encodePassword);

        UserRoleAssignment userRoleAssignment = new UserRoleAssignment();
        Role role = roleRepository.findByName("ROLE_USER");
        
        userRoleAssignment.setRole(role);
        userRoleAssignment.setUser(user);

        userRoleAssignmentRepository.save(userRoleAssignment);

        return loginUser(new LoginRequest(registerRequest.username(), registerRequest.password()));
    }


    @Override
    public String loginUser(LoginRequest loginRequest){
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                 new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password());
            
        Authentication usAuthentication = authenticationManager.authenticate(usernamePasswordAuthenticationToken);

        String jwt = jwtService.generateToken(usAuthentication);

        return jwt;
    }

    

}
