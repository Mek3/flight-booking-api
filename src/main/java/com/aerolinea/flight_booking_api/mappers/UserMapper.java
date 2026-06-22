package com.aerolinea.flight_booking_api.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.aerolinea.flight_booking_api.dtos.RegisterRequest;
import com.aerolinea.flight_booking_api.dtos.UserDTO;
import com.aerolinea.flight_booking_api.models.User;

@Mapper(componentModel = "spring",
         uses = {ReferenceMapper.class},
         unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    UserDTO toUserDTO(User user);
    User toUser(RegisterRequest registerRequest);
    
}
