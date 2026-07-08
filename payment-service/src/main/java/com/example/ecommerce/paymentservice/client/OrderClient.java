package com.example.ecommerce.paymentservice.client;

import com.example.ecommerce.paymentservice.dto.OrderDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ORDER-SERVICE", fallback = OrderClientFallback.class)
public interface OrderClient {

    @GetMapping("/api/orders/id/{id}")
    OrderDto getOrderById(@PathVariable Long id);
}
