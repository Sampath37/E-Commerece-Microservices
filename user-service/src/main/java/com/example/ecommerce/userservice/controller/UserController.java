package com.example.ecommerce.userservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ecommerce.userservice.entity.User;
import com.example.ecommerce.userservice.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        if (user == null || user.getName() == null || user.getEmail() == null) {
            throw new com.example.ecommerce.common.exception.BadRequestException("User, Name, and Email are required");
        }
        try {
            return ResponseEntity.ok(userService.createUser(user));
        } catch (IllegalArgumentException e) {
            throw new com.example.ecommerce.common.exception.BadRequestException(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new com.example.ecommerce.common.exception.ResourceNotFoundException("User not found with id: " + id));
    }

    @GetMapping("/userId/{userId}")
    public ResponseEntity<User> getUserByUserId(@PathVariable String userId) {
        return userService.getUserByUserId(userId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new com.example.ecommerce.common.exception.ResourceNotFoundException("User not found with userId: " + userId));
    }
}
