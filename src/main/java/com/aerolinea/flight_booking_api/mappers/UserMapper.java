package com.aerolinea.flight_booking_api.mappers;

import org.springframework.stereotype.Component;

import com.aerolinea.flight_booking_api.dtos.RegisterRequest;
import com.aerolinea.flight_booking_api.dtos.UserDTO;
import com.aerolinea.flight_booking_api.models.User;

@Component
public class UserMapper {

    public UserDTO toDTO(User user) {
        if (user == null) return null;

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setSurname(user.getSurname());
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getUsername());
        dto.setPhone(user.getPhone());
        
        return dto;
    }

    public User toEntity(UserDTO dto) {
        if (dto == null) return null;

        User user = new User();
        user.setId(dto.getId());
        user.setName(dto.getName());
        user.setSurname(dto.getSurname());
        user.setEmail(dto.getEmail());
        user.setUsername(dto.getUsername());
        user.setPhone(dto.getPhone());
        
        return user;
    }

    public User toEntity(RegisterRequest registerRequest) {
        if (registerRequest == null) return null;

        User user = new User();
        user.setName(registerRequest.name());
        user.setSurname(registerRequest.surname());
        user.setEmail(registerRequest.email());
        user.setUsername(registerRequest.username());
        user.setPhone(registerRequest.phone());
        
        return user;
    }
}
