package com.example.ecommerce.emailservice.service;

import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ecommerce.emailservice.client.OrderClient;
import com.example.ecommerce.emailservice.client.UserClient;
import com.example.ecommerce.emailservice.dto.OrderDto;
import com.example.ecommerce.emailservice.dto.UserDto;
import com.example.ecommerce.emailservice.entity.EmailLog;
import com.example.ecommerce.emailservice.repo.EmailLogRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {

    private final EmailLogRepository emailLogRepository;
    private final JavaMailSender mailSender;
    private final OrderClient orderClient;
    private final UserClient userClient;

    
    public EmailService(EmailLogRepository emailLogRepository, 
                        JavaMailSender mailSender,
                        OrderClient orderClient,
                        UserClient userClient) {
        this.emailLogRepository = emailLogRepository;
        this.mailSender = mailSender;
        this.orderClient = orderClient;
        this.userClient = userClient;
    }

    public List<EmailLog> getAllEmails() {
        return emailLogRepository.findAll();
    }

    @KafkaListener(topics = "${app.kafka.topic.payment-success}", groupId = "email-group")
    @Transactional
    public void consumePaymentSuccessEvent(com.example.ecommerce.emailservice.dto.PaymentDto paymentEvent) {
        try {
            if (paymentEvent == null) return;
            log.info("Email Service consumed PaymentSuccess event: {}", paymentEvent);
            
            Long orderId = paymentEvent.getOrderId();
            if (orderId == null) {
                log.warn("No orderId found in payment event. Skipping email sending.");
                return;
            }

            OrderDto orderDto = orderClient.getOrderById(orderId);
            if (orderDto == null || orderDto.getUserId() == null) {
                log.warn("Order details or userId not found for orderId: {}", orderId);
                return;
            }

            UserDto userDto = userClient.getUserByUserId(orderDto.getUserId());
            if (userDto == null || userDto.getEmail() == null) {
                log.warn("User details or email not found for userId: {}", orderDto.getUserId());
                return;
            }

            String recipientEmail = userDto.getEmail();
            String subject = "Payment Successful - Order #" + orderId;
            String shippingAddress = orderDto.getAddress() != null ? orderDto.getAddress() : (userDto.getAddress() != null ? userDto.getAddress() : "Not Provided");
            String body = "Dear " + (userDto.getName() != null ? userDto.getName() : "Customer") + ",\n\n" +
                          "We have received your payment for order #" + orderId + ".\n" +
                          "Product Name: " + orderDto.getProductCode() + "\n" +
                          "Quantity: " + orderDto.getQuantity() + "\n" +
                          "Payment Status: " + paymentEvent.getStatus() + "\n" +
                          "Shipping Address: " + shippingAddress + "\n\n" +
                          "Thank you for shopping with us!";

            // Send actual email
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipientEmail);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("noreply@ecommerce.com");

            String emailStatus = "FAILED";
            try {
                mailSender.send(message);
                emailStatus = "SENT";
                log.info("Successfully sent email to {}", recipientEmail);
            } catch (Exception ex) {
                log.error("Failed to send email to {}: {}", recipientEmail, ex.getMessage());
            }

            EmailLog emailLog = new EmailLog();
            emailLog.setSubject(subject);
            
            String logBody = body;
            if (logBody.length() > 255) {
                logBody = logBody.substring(0, 252) + "...";
            }
            emailLog.setBody(logBody);
            emailLog.setRecipientEmail(recipientEmail);
            emailLog.setStatus(emailStatus);
            
            emailLogRepository.save(emailLog);
            
        } catch (Exception e) {
            log.error("Error processing PaymentSuccess event: {}", e.getMessage(), e);
        }
    }
}
