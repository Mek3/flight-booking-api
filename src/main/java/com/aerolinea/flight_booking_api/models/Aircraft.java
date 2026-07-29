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

@Table(name ="aircrafts")
@Entity
@SQLDelete(sql = "UPDATE aircrafts SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
public class Aircraft extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "registration_number", nullable = false, unique = true)
    private String registrationNumber;

    @Column(name = "total_flight_hours", nullable = false)
    private Integer totalFlightHours;

    @JoinColumn(name="id_aircraft_model", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private AircraftModel aircraftModel;


    @Builder
    public Aircraft(String registrationNumber, AircraftModel aircraftModel, int totalFlightHours) {
        this.registrationNumber = registrationNumber;
        this.aircraftModel = aircraftModel;
        this.totalFlightHours = totalFlightHours;
    }


    
}
