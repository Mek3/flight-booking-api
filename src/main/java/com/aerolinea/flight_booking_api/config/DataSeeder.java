package com.aerolinea.flight_booking_api.config;

import com.aerolinea.flight_booking_api.repositories.RoleRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.aerolinea.flight_booking_api.models.Flight;
import com.aerolinea.flight_booking_api.models.Role;
import com.aerolinea.flight_booking_api.models.User;
import com.aerolinea.flight_booking_api.models.UserRoleAssignment;
import com.aerolinea.flight_booking_api.repositories.FlightRepository;
import com.aerolinea.flight_booking_api.repositories.UserRepository;
import com.aerolinea.flight_booking_api.repositories.UserRoleAssignmentRepository;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class DataSeeder implements CommandLineRunner{

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final FlightRepository flightRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;

    private final PasswordEncoder passwordEncoder;


    @Override
    public void run(String... args) throws Exception {

        if(roleRepository.count() == 0) {
            Role roleAdmin = new Role();
            roleAdmin.setName("ROLE_ADMIN");
            roleRepository.save(roleAdmin);

            Role roleUser= new Role();
            roleUser.setName("ROLE_USER"); 
            roleRepository.save(roleUser);
        }

       if(userRepository.count() == 0) {
            User testUser = new User();
            testUser.setName("Paco");
            testUser.setSurname("García");
            testUser.setEmail("paco@example.com");
            testUser.setUsername("pacog");
            testUser.setPassword(passwordEncoder.encode("123456")); // Cifraremos esto cuando toque seguridad
            testUser.setPhone("555-1234");

            userRepository.save(testUser);

            UserRoleAssignment userRoleAssignment = new UserRoleAssignment();
            Role role = roleRepository.findByName("ROLE_USER");
            
            userRoleAssignment.setRole(role);
            userRoleAssignment.setUser(testUser);

            userRoleAssignmentRepository.save(userRoleAssignment);

            System.out.println("DataSeeder: Test User created with ID 1.");
       }

        if (flightRepository.count() == 0) {
            Flight testFlight = new Flight();
            testFlight.setFlightNumber("MAD-TOK-001");
            testFlight.setDeparture("Madrid");
            testFlight.setDepartureTime(LocalDateTime.now().plusDays(1)); // Sale mañana
            testFlight.setDestination("Tokyo");
            testFlight.setDestinationTime(LocalDateTime.now().plusDays(1).plusHours(14)); // Llega en 14 horas
            testFlight.setAvaibleSeats(150); 
            testFlight.setPrice(new BigDecimal("299.99")); 

            flightRepository.save(testFlight);
            System.out.println("DataSeeder: Test Flight created with ID 1.");
        }

    }

}
