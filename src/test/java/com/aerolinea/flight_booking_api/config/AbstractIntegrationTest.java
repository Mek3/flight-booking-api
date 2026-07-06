package com.aerolinea.flight_booking_api.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    @SuppressWarnings("resource")
    static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0.33")
                    .withDatabaseName("flight_booking_test")
                    .withUsername("testUser")
                    .withPassword("testpass");
    static {
        mySQLContainer.start();
    }

    @DynamicPropertySource
    static void configureTestProperties(DynamicPropertyRegistry registery) {
        registery.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registery.add("spring.datasource.username", mySQLContainer::getUsername);
        registery.add("spring.datasource.password", mySQLContainer::getPassword);

        registery.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

}
