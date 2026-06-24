package com.example.ecommerce.orderservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "valid_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidUser {
    @Id
    private String userId;
}
