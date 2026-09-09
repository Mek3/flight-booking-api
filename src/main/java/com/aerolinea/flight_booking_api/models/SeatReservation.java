package com.aerolinea.flight_booking_api.models;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.aerolinea.flight_booking_api.exceptions.BusinessRuleViolationException;
import com.aerolinea.flight_booking_api.exceptions.ErrorCode;
import com.aerolinea.flight_booking_api.models.enums.SeatReservationStatus;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "seat_reservations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_seat_reservation_seat_occupied",
                columnNames = {"seat_id", "occupied_flag"}
        )
)
@SQLDelete(sql = "UPDATE seat_reservations SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at is NULL")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeatReservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_segment_id", nullable = false)
    private FlightSegment flightSegment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SeatReservationStatus status;

    @Column(name = "held_until", nullable = false)
    private LocalDateTime heldUntil;

    @Column(name = "occupied_flag", insertable = false, updatable = false)
    private Boolean occupiedFlag;

    @Builder
    public SeatReservation(Seat seat, FlightSegment flightSegment, LocalDateTime heldUntil) {
        this.seat = seat;
        this.flightSegment = flightSegment;
        this.heldUntil = heldUntil;
        this.status = SeatReservationStatus.HELD;
    }

    public void confirm() {
        transitionTo(SeatReservationStatus.CONFIRMED);
    }

    public void expire() {
        transitionTo(SeatReservationStatus.EXPIRED);
    }

    public boolean isExpiredAt(LocalDateTime moment) {
        return this.status == SeatReservationStatus.HELD && moment.isAfter(this.heldUntil);
    }

    private void transitionTo(SeatReservationStatus target) {
        if (!this.status.canTransitionTo(target)) {
            throw new BusinessRuleViolationException(ErrorCode.SEAT_RESERVATION_INVALID_TRANSITION,
                    String.format(ErrorCode.SEAT_RESERVATION_INVALID_TRANSITION.getMessage(),
                            this.id, this.status, target));
        }
        this.status = target;
    }
}