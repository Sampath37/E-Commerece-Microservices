package com.example.ecommerce.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryEventDto {
    private String orderId;
    private String productCode;
    private String status;
    private String reason;
}
