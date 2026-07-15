package com.example.ecommerce.paymentservice.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ecommerce.paymentservice.client.OrderClient;
import com.example.ecommerce.paymentservice.dto.OrderDto;
import com.example.ecommerce.paymentservice.entity.Payment;
import com.example.ecommerce.paymentservice.repo.PaymentRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RefreshScope
public class PaymentService {

	private final PaymentRepository paymentRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final com.example.ecommerce.paymentservice.client.OrderClient orderClient;

	@Value("${app.kafka.topic.payment-success}")
	private String paymentSuccessTopic;

	public PaymentService(PaymentRepository paymentRepository, KafkaTemplate<String, Object> kafkaTemplate,
			OrderClient orderClient) {
		this.paymentRepository = paymentRepository;
		this.kafkaTemplate = kafkaTemplate;
		this.orderClient = orderClient;
	}

	public List<Payment> getAllPayments() {
		return paymentRepository.findAll();
	}

	@KafkaListener(topics = "${app.kafka.topic.inventory-reserved}", groupId = "payment-group")
	@Transactional
	public void consumeInventoryReservedEvent(Map<String, Object> reservedEventMap) {
		try {
			if (reservedEventMap == null || reservedEventMap.isEmpty())
				return;
			log.info("Payment Service consumed InventoryReserved event: {}", reservedEventMap);

			String orderIdStr = (String) reservedEventMap.get("orderId");
			if (orderIdStr != null) {
				Long orderId = Long.valueOf(orderIdStr);

				// Synchronous validation using OpenFeign
				try {
					OrderDto orderDto = orderClient.getOrderById(orderId);
					if (orderDto == null) {
						log.warn("Payment aborted: Order with ID {} could not be verified via API", orderId);
						return;
					}
					log.info("Successfully verified Order ID {} exists via API", orderId);
				} catch (Exception e) {
					log.warn("Payment aborted: Error verifying Order ID {} via API: {}", orderId, e.getMessage());
					return;
				}

				Payment payment = new Payment();
				payment.setOrderId(orderId);
				payment.setStatus("SUCCESS");

				Payment savedPayment = paymentRepository.save(payment);
				log.info("Processed payment for Order ID: {}", orderId);

				// Publish payment-success event
				log.info("Publishing payment-success event to topic: {}", paymentSuccessTopic);
				kafkaTemplate.send(paymentSuccessTopic, savedPayment);
			}
		} catch (Exception e) {
			log.error("Error processing OrderCreated event: {}", e.getMessage(), e);
		}
	}
}
