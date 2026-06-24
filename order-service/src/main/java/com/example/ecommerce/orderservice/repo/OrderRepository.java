package com.example.ecommerce.orderservice.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ecommerce.orderservice.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

	Order findByUserId(String userId);
}
