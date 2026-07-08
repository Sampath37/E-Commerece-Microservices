package com.example.ecommerce.orderservice.client;

import com.example.ecommerce.orderservice.dto.InventoryDto;
import org.springframework.stereotype.Component;

@Component
public class InventoryClientFallback implements InventoryClient {
    @Override
    public InventoryDto getInventoryByProductCode(String productCode) {
        // Return null so the OrderService correctly identifies that the product doesn't exist
        return null;
    }
}
