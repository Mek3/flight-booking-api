package com.aerolinea.flight_booking_api.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;

public abstract class AbstractIntegrationTest {

    @SuppressWarnings("resource")
    static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0.33")
                    .withDatabaseName("flight_booking_test")
                    .withUsername("testUser")
                    .withPassword("testpass");

    @SuppressWarnings("resource")
    static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.0.11")
                    .withExposedPorts(6379);
    static {
        mySQLContainer.start();
        redisContainer.start();
    }

    @DynamicPropertySource
    static void configureTestProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));

        registry.add("spring.flyway.clean-disabled", () -> "false");
    }

    @BeforeEach
    void clearDatabase(@Autowired Flyway flyway) {
        flyway.clean();
        flyway.migrate();
    }

}
