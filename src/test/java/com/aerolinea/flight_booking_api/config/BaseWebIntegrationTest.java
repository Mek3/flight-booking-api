package com.aerolinea.flight_booking_api.config;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.aerolinea.flight_booking_api.models.Role;
import com.aerolinea.flight_booking_api.models.User;
import com.aerolinea.flight_booking_api.services.JwtService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public abstract class BaseWebIntegrationTest extends AbstractIntegrationTest {
    
    @Autowired
    protected WebTestClient webTestClient;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsServiceMock;

    protected String adminJwtToken;

    @BeforeEach
    void setupSecurityContext() {
        String username = "system_admin";
        
        Role adminRole = Role.builder()
                .name("ROLE_ADMIN")
                .description("System Administrator")
                .build();
                
        User adminUser = User.builder()
                .name("System")
                .surname("Admin")
                .email("admin@flightapi.com")
                .username(username)
                .password("dummy_password")
                .phone("555-0199")
                .build();
        
        adminUser.addRole(adminRole);

        UsernamePasswordAuthenticationToken adminAuth = new UsernamePasswordAuthenticationToken(
                adminUser, null, adminUser.getAuthorities()
        );

        this.adminJwtToken = jwtService.generateToken(adminAuth);

        when(userDetailsServiceMock.loadUserByUsername(username)).thenReturn(adminUser);
    }
}
