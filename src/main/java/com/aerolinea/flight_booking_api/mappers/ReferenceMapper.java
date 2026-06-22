package com.aerolinea.flight_booking_api.mappers;

import org.mapstruct.TargetType;
import org.springframework.stereotype.Component;

import com.aerolinea.flight_booking_api.models.BaseEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
public class ReferenceMapper {

    @PersistenceContext
    private EntityManager entityManager;

    public <T extends BaseEntity> T getReference(Long id, @TargetType Class<T> clazz) {

        if(id == null) {
            return null;
        }

        return entityManager.getReference(clazz, id);
    }


}
