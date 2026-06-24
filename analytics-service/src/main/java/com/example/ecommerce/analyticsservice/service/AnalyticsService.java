package com.example.ecommerce.analyticsservice.service;

import java.util.List;
import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ecommerce.analyticsservice.entity.AnalyticsMetric;
import com.example.ecommerce.analyticsservice.repo.AnalyticsMetricRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AnalyticsService {

    private final AnalyticsMetricRepository analyticsMetricRepository;

    
    public AnalyticsService(AnalyticsMetricRepository analyticsMetricRepository) {
        this.analyticsMetricRepository = analyticsMetricRepository;
    }

    public List<AnalyticsMetric> getAllMetrics() {
        return analyticsMetricRepository.findAll();
    }

    @KafkaListener(topics = "${app.kafka.topic.payment-success}", groupId = "analytics-group")
    @Transactional
    public void consumePaymentSuccessEvent(Map<String, Object> paymentEvent) {
        try {
            if (paymentEvent == null || paymentEvent.isEmpty()) return;
            log.info("Analytics Service consumed PaymentSuccess event: {}", paymentEvent);
            // Increment total payments count
            AnalyticsMetric metric = analyticsMetricRepository.findByMetricName("TOTAL_SUCCESSFUL_PAYMENTS")
                    .orElse(new AnalyticsMetric(null, "TOTAL_SUCCESSFUL_PAYMENTS", 0L, null, null));
            
            metric.setMetricValue(metric.getMetricValue() + 1);
            analyticsMetricRepository.save(metric);
            log.info("Analytics updated: TOTAL_SUCCESSFUL_PAYMENTS = {}", metric.getMetricValue());
        } catch (Exception e) {
            log.error("Error processing PaymentSuccess event: {}", e.getMessage(), e);
        }
    }
}
