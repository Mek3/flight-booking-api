package com.aerolinea.flight_booking_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableScheduling
public class FlightBookingApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlightBookingApiApplication.class, args);
	}

}
