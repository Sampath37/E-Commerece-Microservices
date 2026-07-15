package com.example.ecommerce.userservice.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import com.example.ecommerce.userservice.entity.User;
import com.example.ecommerce.userservice.repo.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@Slf4j
@RefreshScope
public class UserService {

	private final UserRepository userRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final PasswordEncoder passwordEncoder;

	@Value("${app.kafka.topic.user-created}")
	private String userCreatedTopic;

	public UserService(UserRepository userRepository, KafkaTemplate<String, Object> kafkaTemplate,
			PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.kafkaTemplate = kafkaTemplate;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public User createUser(User user) {
		if (userRepository.findByEmail(user.getEmail()).isPresent()) {
			throw new IllegalArgumentException("User with email " + user.getEmail() + " already exists");
		}

		if (user.getPassword() != null) {
			user.setPassword(passwordEncoder.encode(user.getPassword()));
		}

		User savedUser = userRepository.save(user);

		// Publish Event
		log.info("Publishing UserCreated event to topic: {} for User ID: {}", userCreatedTopic, savedUser.getId());
		kafkaTemplate.send(userCreatedTopic, savedUser);

		return savedUser;
	}

	public List<User> getAllUsers() {
		return userRepository.findAll();
	}

	public Optional<User> getUserById(Long id) {
		return userRepository.findById(id);
	}

	public Optional<User> getUserByUserId(String userId) {
		return userRepository.findByUserId(userId);
	}

	public User authenticate(String email, String password) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

		if (!passwordEncoder.matches(password, user.getPassword())) {
			throw new IllegalArgumentException("Invalid email or password");
		}

		return user;
	}

	@Transactional
	public User changePassword(String email, String oldPassword, String newPassword) {
		User user = authenticate(email, oldPassword);
		user.setPassword(passwordEncoder.encode(newPassword));
		return userRepository.save(user);
	}

	@Transactional
	public void deleteUser(String userId) {
		User user = userRepository.findByUserId(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found with userId: " + userId));
		userRepository.delete(user);
		log.info("Deleted User with userId: {}", userId);
	}
}
