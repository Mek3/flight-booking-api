package com.aerolinea.flight_booking_api.models;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="flights")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Flight  extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flight_number", nullable = false, unique = true)
    private String flightNumber;

    @Column(name = "departure", nullable = false)
    private String departure;

    @Column(name = "departure_time", nullable = false)
    private Date departureTime;

    @Column(name = "destination", nullable = false)
    private String destination;
    
    @Column(name="destination_time", nullable = false)
    private Date destinationTime;

    @Column(name = "avaible_seats", nullable = false) 
    private Integer avaibleSeats;

    @Column(name = "price", nullable = false)
    private Double price;

}