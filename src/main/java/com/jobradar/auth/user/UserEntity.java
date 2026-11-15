package com.jobradar.auth.user;

import java.util.HashSet;
import java.util.Set;

import com.jobradar.auth.role.AppRoleEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "username", nullable = false, length = 100, unique = true)
    private String email;

    @Column(name = "password", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "full_name", length = 150, columnDefinition = "nvarchar(150)")
    private String fullName;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "username", referencedColumnName = "username"),
            inverseJoinColumns = @JoinColumn(name = "role", referencedColumnName = "role"))
    private Set<AppRoleEntity> roles = new HashSet<>();

    protected UserEntity() {
    }

    public UserEntity(String fullName, String email, String passwordHash) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public Integer getId() {
        return id;
    }

    void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public Set<AppRoleEntity> getRoles() {
        return roles;
    }
}
