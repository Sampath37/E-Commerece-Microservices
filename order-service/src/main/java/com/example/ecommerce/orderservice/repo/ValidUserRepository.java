package com.example.ecommerce.orderservice.repo;

import com.example.ecommerce.orderservice.entity.ValidUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ValidUserRepository extends JpaRepository<ValidUser, String> {
}
