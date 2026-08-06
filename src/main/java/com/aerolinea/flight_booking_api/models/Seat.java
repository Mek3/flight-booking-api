package com.aerolinea.flight_booking_api.models;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "seats",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_flight_seat",
                        columnNames = {"flight_instance_id", "seat_row", "seat_letter"}
                )
        }
)
@SQLDelete(sql = "UPDATE seats SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at is NULL")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_instance_id", nullable = false)
    private FlightInstance flightInstance;

    @Column(name = "seat_row", nullable = false)
    private Integer rowNumber;

    @Column(name = "seat_letter", nullable = false, length = 5)
    private String seatLetter;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable;

    @Builder
    public Seat(FlightInstance flightInstance, Integer rowNumber, String seatLetter, Boolean isAvailable) {
        this.flightInstance = flightInstance;
        this.rowNumber = rowNumber;
        this.seatLetter = seatLetter;
        this.isAvailable = isAvailable != null ? isAvailable : true;
    }
}