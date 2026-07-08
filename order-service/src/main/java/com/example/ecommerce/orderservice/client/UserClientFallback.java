package com.example.ecommerce.orderservice.client;

import com.example.ecommerce.orderservice.dto.UserDto;
import org.springframework.stereotype.Component;

@Component
public class UserClientFallback implements UserClient {
    @Override
    public UserDto getUserByUserId(String userId) {
        // Return null so the OrderService correctly identifies that the user doesn't exist
        return null;
    }
}
