package com.example.ecommerce.analyticsservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ecommerce.analyticsservice.entity.AnalyticsMetric;
import com.example.ecommerce.analyticsservice.service.AnalyticsService;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

   
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public ResponseEntity<List<AnalyticsMetric>> getAllMetrics() {
        return ResponseEntity.ok(analyticsService.getAllMetrics());
    }
}
