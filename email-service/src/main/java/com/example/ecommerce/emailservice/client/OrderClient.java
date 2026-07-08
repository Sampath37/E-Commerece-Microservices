package com.example.ecommerce.emailservice.client;

import com.example.ecommerce.emailservice.dto.OrderDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service")
public interface OrderClient {
    
    @GetMapping("/api/orders/id/{id}")
    OrderDto getOrderById(@PathVariable Long id);
}
