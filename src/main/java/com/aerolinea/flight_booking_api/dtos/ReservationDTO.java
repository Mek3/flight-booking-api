package com.aerolinea.flight_booking_api.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.aerolinea.flight_booking_api.models.ReservationStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDTO {

    private Long id;
    private Long userId;
    private Long flightId;
    private Integer numberOfPassengers;

    private String reservationCode;
    private ReservationStatus status;
    private BigDecimal totalPrice;
    private LocalDateTime createdAt;

}
