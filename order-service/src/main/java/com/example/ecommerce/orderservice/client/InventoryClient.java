package com.example.ecommerce.orderservice.client;

import com.example.ecommerce.orderservice.dto.InventoryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "INVENTORY-SERVICE", fallback = InventoryClientFallback.class)
public interface InventoryClient {

    @GetMapping("/api/inventory/product/{productCode}")
    InventoryDto getInventoryByProductCode(@PathVariable String productCode);
}
