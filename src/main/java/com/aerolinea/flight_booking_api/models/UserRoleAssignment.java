package com.aerolinea.flight_booking_api.models;

import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "users_roles", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "role_id"})})
@SQLDelete(sql = "UPDATE users_roles SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")  
@SQLRestriction("deleted_at is NULL")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRoleAssignment extends BaseEntity{
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Builder
    public UserRoleAssignment(User user, Role role) {
        this.user = user;
        this.role = role;
    }

}
