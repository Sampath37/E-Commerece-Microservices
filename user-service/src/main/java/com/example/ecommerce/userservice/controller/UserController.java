package com.example.ecommerce.userservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ecommerce.common.exception.BadRequestException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.userservice.dto.ChangePasswordRequest;
import com.example.ecommerce.userservice.dto.LoginRequest;
import com.example.ecommerce.userservice.dto.LoginResponse;
import com.example.ecommerce.userservice.entity.User;
import com.example.ecommerce.userservice.service.JwtService;
import com.example.ecommerce.userservice.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;
	private final JwtService jwtService;

	public UserController(UserService userService, JwtService jwtService) {
		this.userService = userService;
		this.jwtService = jwtService;
	}

	@PostMapping
	public ResponseEntity<User> createUser(@RequestBody User user) {
		if (user == null || user.getName() == null || user.getEmail() == null) {
			throw new BadRequestException("User, Name, and Email are required");
		}
		try {
			return ResponseEntity.ok(userService.createUser(user));
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e.getMessage());
		}
	}

	@GetMapping
	public ResponseEntity<List<User>> getAllUsers() {
		return ResponseEntity.ok(userService.getAllUsers());
	}

	@GetMapping("/{id}")
	public ResponseEntity<User> getUserById(@PathVariable Long id) {
		return userService.getUserById(id).map(ResponseEntity::ok)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
	}

	@GetMapping("/userId/{userId}")
	public ResponseEntity<User> getUserByUserId(@PathVariable String userId) {
		return userService.getUserByUserId(userId).map(ResponseEntity::ok)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with userId: " + userId));
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
		try {
			User user = userService.authenticate(request.getEmail(), request.getPassword());
			String token = jwtService.generateToken(user.getUserId());
			return ResponseEntity.ok(new LoginResponse(token, user.getUserId()));
		} catch (IllegalArgumentException e) {
			throw new BadRequestException("Invalid credentials");
		}
	}

	@PostMapping("/change-password")
	public ResponseEntity<User> changePassword(@RequestBody ChangePasswordRequest request) {
		try {
			User updatedUser = userService.changePassword(request.getEmail(), request.getOldPassword(),
					request.getNewPassword());
			return ResponseEntity.ok(updatedUser);
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e.getMessage());
		}
	}

	@DeleteMapping("/{userId}")
	public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
		try {
			userService.deleteUser(userId);
			return ResponseEntity.ok().build();
		} catch (IllegalArgumentException e) {
			throw new ResourceNotFoundException(e.getMessage());
		}
	}
}
