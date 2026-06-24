package com.example.ecommerce.paymentservice.client;

import com.example.ecommerce.paymentservice.dto.OrderDto;
import org.springframework.stereotype.Component;

@Component
public class OrderClientFallback implements OrderClient {
    @Override
    public OrderDto getOrderById(Long id) {
        OrderDto fallback = new OrderDto();
        fallback.setId(id);
        fallback.setStatus("UNKNOWN");
        return fallback;
    }
}
