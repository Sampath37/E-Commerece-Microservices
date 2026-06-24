package com.example.ecommerce.userservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String userId;
    private String name;
    private String email;
    private String address;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (userId == null) {
            userId = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        }
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}
