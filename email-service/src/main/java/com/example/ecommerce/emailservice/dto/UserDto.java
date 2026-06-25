package com.example.ecommerce.emailservice.dto;

import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String userId;
    private String name;
    private String email;
    private String address;
}
