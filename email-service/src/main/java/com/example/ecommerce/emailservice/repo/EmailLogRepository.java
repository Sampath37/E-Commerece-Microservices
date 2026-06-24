package com.example.ecommerce.emailservice.repo;

import com.example.ecommerce.emailservice.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
}
