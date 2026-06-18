package com.aerolinea.flight_booking_api.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlightDTO {

    private Long id;

    @NotBlank
    private String flightNumber;

    @NotBlank
    private String departure;

    @NotNull
    private LocalDateTime departureTime;

    @NotBlank
    private String destination;
    
    @NotNull
    private LocalDateTime destinationTime;

    @NotNull
    private Integer availableSeats;

    @NotNull
    private BigDecimal price;

}
