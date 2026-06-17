package com.aerolinea.flight_booking_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class FlightBookingApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlightBookingApiApplication.class, args);
	}

}
