package com.example.ecommerce.orderservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ecommerce.common.exception.BadRequestException;
import com.example.ecommerce.orderservice.entity.Order;
import com.example.ecommerce.orderservice.service.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        if (order == null || order.getProductCode() == null || order.getQuantity() == null) {
            throw new BadRequestException("Order, ProductCode, and Quantity are required");
        }
        try {
            return ResponseEntity.ok(orderService.createOrder(order));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{userId}")
	public ResponseEntity<List<Order>> getOrderById(@PathVariable String userId) {
		return ResponseEntity.ok(orderService.getOrderById(userId));
	}

    @GetMapping("/id/{id}")
    public ResponseEntity<Order> getOrderByOrderId(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderByOrderId(id));
    }
}
