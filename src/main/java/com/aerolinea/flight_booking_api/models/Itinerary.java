package com.aerolinea.flight_booking_api.models;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "itineraries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_itinerary_reservation_sequence_active",
                columnNames = {"reservation_id", "sequence_order", "active_flag"}
        )
)
@SQLDelete(sql = "UPDATE itineraries SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at is NULL")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Itinerary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Column(name = "active_flag", insertable = false, updatable = false)
    private Boolean activeFlag;

    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("segmentOrder ASC")
    private List<FlightSegment> segments = new ArrayList<>();

    @Builder
    public Itinerary(Reservation reservation, Integer sequenceOrder) {
        this.reservation = reservation;
        this.sequenceOrder = sequenceOrder;
    }

    public void addSegment(FlightSegment segment) {
        segment.setItinerary(this);
        segment.setSegmentOrder(this.segments.size() + 1);
        this.segments.add(segment);
    }

    public void removeSegment(FlightSegment segment) {
        this.segments.remove(segment);
        segment.setItinerary(null);
    }

    public FlightSegment firstSegment() {
        return this.segments.isEmpty() ? null : this.segments.get(0);
    }

    public FlightSegment lastSegment() {
        return this.segments.isEmpty() ? null : this.segments.get(this.segments.size() - 1);
    }

    public boolean isDirect() {
        return this.segments.size() == 1;
    }
}