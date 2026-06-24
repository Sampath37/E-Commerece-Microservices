package com.example.ecommerce.orderservice.client;

import com.example.ecommerce.orderservice.dto.InventoryDto;
import org.springframework.stereotype.Component;

@Component
public class InventoryClientFallback implements InventoryClient {
    @Override
    public InventoryDto getInventoryByProductCode(String productCode) {
        InventoryDto fallback = new InventoryDto();
        fallback.setProductCode(productCode);
        fallback.setStock(0); // Fallback to 0 stock
        return fallback;
    }
}
