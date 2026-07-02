package com.aerolinea.flight_booking_api.services;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.aerolinea.flight_booking_api.dtos.LoginRequest;
import com.aerolinea.flight_booking_api.dtos.RegisterRequest;
import com.aerolinea.flight_booking_api.exceptions.BusinessRuleViolationException;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
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
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;


    @Override
    public String registerUser(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.username())) {
            throw new BusinessRuleViolationException(ErrorCode.USER_ALREADY_EXISTS, 
            String.format(ErrorCode.USER_ALREADY_EXISTS.getMessage(), registerRequest.username()));
        }

        User user = User.builder()
                .name(registerRequest.name())
                .surname(registerRequest.surname())
                .email(registerRequest.email())
                .username(registerRequest.username())
                .password(passwordEncoder.encode(registerRequest.password()))
                .phone(registerRequest.phone())
                .build();

        UserRoleAssignment userRoleAssignment = UserRoleAssignment.builder()
                                        .role(roleRepository.findByName("ROLE_USER"))
                                        .user(user).build();
        
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
