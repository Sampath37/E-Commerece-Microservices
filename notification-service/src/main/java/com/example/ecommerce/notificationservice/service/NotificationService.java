package com.example.ecommerce.notificationservice.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.example.ecommerce.notificationservice.entity.Notification;
import com.example.ecommerce.notificationservice.repo.NotificationRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

   
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    @KafkaListener(topics = "${app.kafka.topic.user-created}", groupId = "notification-group")
    public void consumeUserCreatedEvent(Object userEvent) {
        try {
            if (userEvent == null) return;
            log.info("Notification Service consumed UserCreated event: {}", userEvent);
            Notification notification = new Notification();
            String msg = "User created: " + userEvent.toString();
            if (msg.length() > 255) {
                msg = msg.substring(0, 252) + "...";
            }
            notification.setMessage(msg);
            notification.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(notification);
            log.info("Saved notification for UserCreated");
        } catch (Exception e) {
            log.error("Error processing UserCreated event: {}", e.getMessage(), e);
        }
    }

}
