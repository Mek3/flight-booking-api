package com.aerolinea.flight_booking_api.models;

import java.time.LocalDateTime;

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
@Table(
        name = "flight_segments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_segment_itinerary_order_active",
                columnNames = {"itinerary_id", "segment_order", "active_flag"}
        )
)
@SQLDelete(sql = "UPDATE flight_segments SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at is NULL")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlightSegment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id", nullable = false)
    private Itinerary itinerary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_instance_id", nullable = false)
    private FlightInstance flightInstance;

    @Column(name = "segment_order", nullable = false)
    private Integer segmentOrder;

    @Column(name = "active_flag", insertable = false, updatable = false)
    private Boolean activeFlag;

    @Builder
    public FlightSegment(Itinerary itinerary, FlightInstance flightInstance, Integer segmentOrder) {
        this.itinerary = itinerary;
        this.flightInstance = flightInstance;
        this.segmentOrder = segmentOrder;
    }

    public LocalDateTime getDepartureAt() {
        return flightInstance.getDepartureAt();
    }

    public LocalDateTime getArrivalAt() {
        return flightInstance.getArrivalAt();
    }

    public Airport getDepartureAirport() {
        return flightInstance.getFlightSchedule().getDepartureAirport();
    }

    public Airport getArrivalAirport() {
        return flightInstance.getFlightSchedule().getArrivalAirport();
    }
}