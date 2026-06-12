package com.aerolinea.flight_booking_api.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public class BaseEntity {

    @Column(name="created_at", nullable=false, updatable=false)
    protected LocalDateTime createdAt;

    @Column(name= "created_by", nullable = false, updatable = false)
    protected String createdBy;

    @Column(name = "update_at")
    protected LocalDateTime updateAt;

    @Column(name="update_by")
    protected String updateBy;

    @Column(name = "delete_at")
    protected LocalDateTime deleteAt;

    @Column(name="delete_by")
    protected String deleteBy;

    @PrePersist
    protected void onCreated(){
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateAt = LocalDateTime.now();
    }

}
