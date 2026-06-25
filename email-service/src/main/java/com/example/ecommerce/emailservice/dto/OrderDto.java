package com.example.ecommerce.emailservice.dto;

import lombok.Data;

@Data
public class OrderDto {
    private Long id;
    private String userId;
    private String productCode;
    private Integer quantity;
    private String status;
    private String address;
}
