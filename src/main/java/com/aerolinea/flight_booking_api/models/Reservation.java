package com.aerolinea.flight_booking_api.models;

import java.math.BigDecimal;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.aerolinea.flight_booking_api.exceptions.BusinessRuleViolationException;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reservations")
@SQLDelete(sql = "UPDATE reservations SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at is null")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_code", nullable = false, unique = true) 
    private String reservationCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ReservationStatus status;

    @Column(name = "number_of_passengers", nullable = false)
    private Integer numberOfPassengers;
    
    @Column(name="total_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalPrice;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;


    @Builder
    public Reservation(String reservationCode, ReservationStatus status, Integer numberOfPassengers, 
                       BigDecimal totalPrice, User user, Flight flight) {
        this.reservationCode = reservationCode;
        this.status = status;
        this.numberOfPassengers = numberOfPassengers;
        this.totalPrice = totalPrice;
        this.user = user;
        this.flight = flight;
    }

    public void confirmReservation() {
        if (this.status == ReservationStatus.CONFIRMED) {
            throw new BusinessRuleViolationException(ErrorCode.RESERVATION_ALREADY_CONFIRMED,
                    String.format(ErrorCode.RESERVATION_ALREADY_CONFIRMED.getMessage(), this.id));
        }
        this.status = ReservationStatus.CONFIRMED;
    }

    public void cancelReservation() {
        if (this.status == ReservationStatus.CANCELLED) {
            throw new BusinessRuleViolationException(ErrorCode.RESERVATION_ALREADY_CANCELLED,
                    String.format(ErrorCode.RESERVATION_ALREADY_CANCELLED.getMessage(), this.id));
        }
        this.status = ReservationStatus.CANCELLED;
    }

    public void expireReservation() {
        if (this.status == ReservationStatus.EXPIRED) {
            throw new BusinessRuleViolationException(ErrorCode.RESERVATION_ALREADY_EXPIRED,
                    String.format(ErrorCode.RESERVATION_ALREADY_EXPIRED.getMessage(), this.id));
        }
        this.status = ReservationStatus.EXPIRED;
    }


}
