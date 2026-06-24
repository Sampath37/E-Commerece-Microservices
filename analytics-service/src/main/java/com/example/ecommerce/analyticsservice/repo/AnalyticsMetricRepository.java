package com.example.ecommerce.analyticsservice.repo;

import com.example.ecommerce.analyticsservice.entity.AnalyticsMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalyticsMetricRepository extends JpaRepository<AnalyticsMetric, Long> {
    Optional<AnalyticsMetric> findByMetricName(String metricName);
}
