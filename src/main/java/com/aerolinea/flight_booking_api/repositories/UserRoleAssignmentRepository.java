package com.aerolinea.flight_booking_api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aerolinea.flight_booking_api.models.UserRoleAssignment;

public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignment, Long>{

}
