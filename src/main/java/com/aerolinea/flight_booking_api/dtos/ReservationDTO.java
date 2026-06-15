package com.aerolinea.flight_booking_api.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDTO {

    private Long Id;
    private Long userId;
    private Long flightId;
    private Integer numberOfPassengers;

  

}
