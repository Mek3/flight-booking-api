package com.aerolinea.flight_booking_api.models;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "aircraft_models",
       uniqueConstraints = {
            @UniqueConstraint(
                name = "uk_manufacturer_model",
                columnNames = {"manufacturer", "model_name"}
            )
       }
)
@SQLDelete(sql = "UPDATE aircraft_models SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at is NULL")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AircraftModel extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name="manufacturer", nullable = false, length = 50)
    private String manufacturer;

    @Column(name="model_name", nullable = false, length = 50)
    private String modelName;
    
    @Column(name="max_capacity", nullable = false)
    private Short maxCapacity;

    @Builder
    public AircraftModel(String manufacturer, String modelName, Short maxCapacity) {
        this.manufacturer = manufacturer;
        this.modelName = modelName;
        this.maxCapacity = maxCapacity;
    }

}
