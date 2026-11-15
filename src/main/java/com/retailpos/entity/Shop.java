package com.retailpos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name="shops")
@Getter
@Setter
@NoArgsConstructor
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name="created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Shop(String name, String slug){
        this.name = name;
        this.slug = slug;
        this.createdAt = Instant.now();
    }

}