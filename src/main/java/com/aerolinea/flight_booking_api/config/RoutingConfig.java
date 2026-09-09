package com.aerolinea.flight_booking_api.config;

import com.aerolinea.flight_booking_api.services.routing.RoutingValidator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RoutingProperties.class)
public class RoutingConfig {

    @Bean
    public RoutingValidator routingValidator(RoutingProperties routingProperties) {
        return new RoutingValidator(routingProperties.toPolicy());
    }
}