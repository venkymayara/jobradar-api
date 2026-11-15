package com.jobradar.auth.role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_roles")
public class AppRoleEntity {
    @Id
    @Column(name = "role", nullable = false, length = 255)
    private String role;

    protected AppRoleEntity() {
    }

    public AppRoleEntity(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof AppRoleEntity that && role.equals(that.role));
    }

    @Override
    public int hashCode() {
        return role.hashCode();
    }
}
