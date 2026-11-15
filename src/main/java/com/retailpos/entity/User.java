package com.retailpos.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name="users")
@NoArgsConstructor
@Getter
@Setter
public class User{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Setter(AccessLevel.NONE)
    @Column(name="password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name="created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public User(String email, String passwordHash){
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
    }
}