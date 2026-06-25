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

@Service
@Slf4j
@RefreshScope
public class UserService {

    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${app.kafka.topic.user-created}")
    private String userCreatedTopic;

    
    public UserService(UserRepository userRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.userRepository = userRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public User createUser(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User with email " + user.getEmail() + " already exists");
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
}
