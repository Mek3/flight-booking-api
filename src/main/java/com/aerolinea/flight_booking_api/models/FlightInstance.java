package com.aerolinea.flight_booking_api.models;

import java.time.LocalDate;

import com.aerolinea.flight_booking_api.models.enums.FlightStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "flight_instances",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_instance_schedule_date_active",
                columnNames = {"flight_schedule_id", "departure_date", "active_flag"}
        )
)
@SQLDelete(sql = "UPDATE flight_instances SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at is NULL")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlightInstance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_schedule_id", nullable = false)
    private FlightSchedule flightSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aircraft_id")
    private Aircraft aircraft;

    @Column(name = "departure_date", nullable = false)
    private LocalDate departureDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private FlightStatus status;

    @Column(name = "active_flag", insertable = false, updatable = false)
    private Boolean activeFlag;

    @Builder
    public FlightInstance(FlightSchedule flightSchedule, Aircraft aircraft, LocalDate departureDate, FlightStatus status) {
        this.flightSchedule = flightSchedule;
        this.aircraft = aircraft;
        this.departureDate = departureDate;
        this.status = status;
    }
}