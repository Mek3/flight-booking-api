package com.aerolinea.flight_booking_api.config;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
public abstract class AbstractJpaTest extends AbstractIntegrationTest {

    @Autowired
    protected TestEntityManager entityManager;

    @MockitoBean(name = "auditorAware")
    private AuditorAware<String> auditorAware;

    @BeforeEach
    void setupAuditor() {
        Mockito.when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("test_admin"));
    }
}
