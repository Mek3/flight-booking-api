package com.aerolinea.flight_booking_api.models;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User  extends BaseEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "surname", nullable = false)
    private String surname;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "phone")
    private String phone;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER)
    private List<UserRoleAssignment> roles;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if(roles == null) 
            return Collections.emptyList();

        return roles.stream().map(assignment -> new SimpleGrantedAuthority(assignment.getRole().getName())).toList();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Asumimos que las cuentas no caducan
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Asumimos que la cuenta no se bloquea por intentos fallidos
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Asumimos que las contraseñas no caducan
    }

    @Override
    public boolean isEnabled() {
        return this.isActive; // Vinculamos la seguridad de Spring a tu columna "is_active"
    }
}
