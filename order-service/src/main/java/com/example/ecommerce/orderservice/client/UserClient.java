package com.example.ecommerce.orderservice.client;

import com.example.ecommerce.orderservice.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "USER-SERVICE", fallback = UserClientFallback.class)
public interface UserClient {

    @GetMapping("/api/users/userId/{userId}")
    UserDto getUserByUserId(@PathVariable("userId") String userId);
}
