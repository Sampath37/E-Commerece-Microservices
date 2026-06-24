package com.example.ecommerce.emailservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ecommerce.emailservice.entity.EmailLog;
import com.example.ecommerce.emailservice.service.EmailService;

@RestController
@RequestMapping("/api/emails")
public class EmailController {

    private final EmailService emailService;

    
    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping
    public ResponseEntity<List<EmailLog>> getAllEmails() {
        return ResponseEntity.ok(emailService.getAllEmails());
    }
}
