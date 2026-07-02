package com.aerolinea.flight_booking_api.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.aerolinea.flight_booking_api.exceptions.BusinessRuleViolationException;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name="flights")
@SQLDelete(sql = "UPDATE flights SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at is NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Flight  extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flight_number", nullable = false, unique = true)
    private String flightNumber;

    @Column(name = "departure", nullable = false)
    private String departure;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "destination", nullable = false)
    private String destination;
    
    @Column(name="destination_time", nullable = false)
    private LocalDateTime destinationTime;

    @Column(name = "available_seats", nullable = false) 
    private Integer availableSeats;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Version
    private Long version;

    @Builder
    public Flight(String flightNumber, String departure, LocalDateTime departureTime, 
                  String destination, LocalDateTime destinationTime, Integer availableSeats, BigDecimal price) {
        this.flightNumber = flightNumber;
        this.departure = departure;
        this.departureTime = departureTime;
        this.destination = destination;
        this.destinationTime = destinationTime;
        this.availableSeats = availableSeats;
        this.price = price;
    }

    public void decreaseAvailableSeats(int seats) {
        if (availableSeats >= seats) {
            availableSeats -= seats;
        } else {
            throw new BusinessRuleViolationException(ErrorCode.NOT_ENOUGH_SEATS, String.format(ErrorCode.NOT_ENOUGH_SEATS.getMessage(), id));
        }
    }

    public void increaseAvailableSeats(int seats) {
        availableSeats += seats;
    }

}