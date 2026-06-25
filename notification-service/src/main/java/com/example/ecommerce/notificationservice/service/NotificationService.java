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
    @KafkaListener(topics = "${app.kafka.topic.order-created}", groupId = "notification-group")
    public void consumeOrderCreatedEvent(Object orderEvent) {
        try {
            if (orderEvent == null) return;
            log.info("Notification Service consumed OrderCreated event: {}", orderEvent);
            Notification notification = new Notification();
            String msg = "Order confirmation: " + orderEvent.toString();
            if (msg.length() > 255) {
                msg = msg.substring(0, 252) + "...";
            }
            notification.setMessage(msg);
            notification.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(notification);
            log.info("Saved notification for OrderCreated");
        } catch (Exception e) {
            log.error("Error processing OrderCreated event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "${app.kafka.topic.payment-success}", groupId = "notification-group")
    public void consumePaymentEvent(@org.springframework.messaging.handler.annotation.Payload java.util.Map<String, Object> paymentEvent) {
        try {
            if (paymentEvent == null) return;
            log.info("Notification Service consumed Payment event: {}", paymentEvent);
            
            Object statusObj = paymentEvent.get("status");
            String status = statusObj != null ? statusObj.toString() : "UNKNOWN";
            
            Notification notification = new Notification();
            String msg;
            if ("SUCCESS".equalsIgnoreCase(status)) {
                msg = "Payment successful: " + paymentEvent.toString();
            } else {
                msg = "Payment failed: " + paymentEvent.toString();
            }

            if (msg.length() > 255) {
                msg = msg.substring(0, 252) + "...";
            }
            notification.setMessage(msg);
            notification.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(notification);
            log.info("Saved notification for Payment with status {}", status);
        } catch (Exception e) {
            log.error("Error processing Payment event: {}", e.getMessage(), e);
        }
    }

}
