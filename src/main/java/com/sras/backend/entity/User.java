package com.sras.backend.entity;

import com.sras.backend.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User implements Persistable<Integer> {

    /**
     * Application-assigned 6-digit employee ID (503xxx).
     * Not database-generated, so Persistable is used to tell Spring Data
     * whether to INSERT (persist) or UPDATE (merge).
     */
    @Id
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "full_name", length = 50, nullable = false)
    private String fullName;

    @Column(name = "password_hash", length = 60, nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 10, nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Transient
    @Getter(lombok.AccessLevel.NONE)
    @Setter(lombok.AccessLevel.NONE)
    private boolean newEntity = true;

    @Override
    public Integer getId() {
        return userId;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.newEntity = false;
    }
}
