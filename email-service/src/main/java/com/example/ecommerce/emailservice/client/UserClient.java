package com.example.ecommerce.emailservice.client;

import com.example.ecommerce.emailservice.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/api/users/userId/{userId}")
    UserDto getUserByUserId(@PathVariable String userId);
}
