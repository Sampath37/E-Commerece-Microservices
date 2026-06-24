package com.example.ecommerce.orderservice.client;

import com.example.ecommerce.orderservice.dto.UserDto;
import org.springframework.stereotype.Component;

@Component
public class UserClientFallback implements UserClient {
    @Override
    public UserDto getUserByUserId(String userId) {
        UserDto fallback = new UserDto();
        fallback.setUserId(userId);
        fallback.setName("Fallback User");
        return fallback;
    }
}
