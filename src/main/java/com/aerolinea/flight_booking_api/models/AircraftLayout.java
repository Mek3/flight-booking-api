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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "aircraft_layouts")
@SQLDelete(sql = "UPDATE aircraft_layouts SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at is NULL")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AircraftLayout extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aircraft_model_id", nullable = false)
    private AircraftModel aircraftModel;

    @Column(name = "cabin_class", nullable = false, length = 50)
    private String cabinClass;

    @Column(name = "seat_capacity", nullable = false)
    private Integer seatCapacity;

    @Column(name = "total_rows", nullable = false)
    private Integer totalRows;

    @Column(name = "seat_letters", nullable = false, length = 10)
    private String seatLetters;

    @Builder
    public AircraftLayout(AircraftModel aircraftModel, String cabinClass, Integer seatCapacity, Integer totalRows, String seatLetters) {
        this.aircraftModel = aircraftModel;
        this.cabinClass = cabinClass;
        this.seatCapacity = seatCapacity;
        this.totalRows = totalRows;
        this.seatLetters = seatLetters;

    }
}