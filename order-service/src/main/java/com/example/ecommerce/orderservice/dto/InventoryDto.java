package com.example.ecommerce.orderservice.dto;

import lombok.Data;

@Data
public class InventoryDto {
    private Long id;
    private String productCode;
    private Integer stock;
    private String status;

}
