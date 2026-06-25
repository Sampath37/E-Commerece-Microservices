package com.example.ecommerce.orderservice.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.ecommerce.orderservice.client.UserClient;
import com.example.ecommerce.orderservice.dto.InventoryEventDto;
import com.example.ecommerce.orderservice.dto.UserDto;
import com.example.ecommerce.orderservice.entity.Order;
import com.example.ecommerce.orderservice.entity.ValidUser;
import com.example.ecommerce.orderservice.repo.OrderRepository;
import com.example.ecommerce.orderservice.repo.ValidUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RefreshScope
public class OrderService {

	private final OrderRepository orderRepository;
	private final ValidUserRepository validUserRepository; // Keeping for legacy/Kafka event
	private final KafkaTemplate<String, Object> kafkaTemplate;

	private final UserClient userClient;
	private final ObjectMapper objectMapper;

	@Value("${app.kafka.topic.order-created}")
	private String orderCreatedTopic;

	public OrderService(OrderRepository orderRepository, ValidUserRepository validUserRepository,
			KafkaTemplate<String, Object> kafkaTemplate, UserClient userClient, ObjectMapper objectMapper) {
		this.orderRepository = orderRepository;
		this.validUserRepository = validUserRepository;
		this.kafkaTemplate = kafkaTemplate;

		this.userClient = userClient;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public Order createOrder(Order order) {
		if (order == null || order.getUserId() == null || order.getUserId().trim().isEmpty()) {
			throw new IllegalArgumentException("Order and UserId cannot be null or empty");
		}

		// Synchronous User Validation via OpenFeign
		try {
			UserDto userDto = userClient.getUserByUserId(order.getUserId());
			if (userDto == null) {
				throw new IllegalArgumentException("User with ID " + order.getUserId() + " does not exist!");
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("User validation failed or User does not exist: " + e.getMessage());
		}

		// Asynchronous validation using Saga Pattern
		// The inventory check will be done asynchronously by the inventory-service

		order.setStatus("PENDING");
		Order savedOrder = orderRepository.save(order);

		// Publish Event AFTER the database transaction commits
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				log.info("Publishing OrderCreated event to topic: {} for Order ID: {}", orderCreatedTopic,
						savedOrder.getId());
				kafkaTemplate.send(orderCreatedTopic, savedOrder);
			}
		});

		return savedOrder;
	}

	public List<Order> getAllOrders() {
		return orderRepository.findAll();
	}

	public Order getOrderById(String userId) {
		if (userId != null) {
			Order order = orderRepository.findByUserId(userId);
			if (order != null) {
				return order;
			} else {
				log.error("UserId is wrong {}", userId);
				return null;
			}
		} else {
			log.error("userId is null {}", userId);
		}
		return null;
	}

	public Order getOrderByOrderId(Long orderId) {
		if (orderId != null) {
			return orderRepository.findById(orderId).orElse(null);
		} else {
			log.error("orderId is null");
		}
		return null;
	}

	@KafkaListener(topics = "${app.kafka.topic.user-created}", groupId = "order-group")
	public void consumeUserCreatedEvent(Map<String, Object> userEvent) {
		try {
			if (userEvent == null || userEvent.isEmpty())
				return;
			log.info("Order Service consumed UserCreated event: {}", userEvent);

			String userId = (String) userEvent.get("userId");
			if (userId != null && !userId.trim().isEmpty()) {
				validUserRepository.save(new ValidUser(userId));
				log.info("Saved valid user to local cache: {}", userId);
			}
		} catch (Exception e) {
			log.error("Error processing UserCreated event: {}", e.getMessage(), e);
		}
	}

	@KafkaListener(topics = "${app.kafka.topic.inventory-reserved}", groupId = "order-group")
	@Transactional
	public void consumeInventoryReservedEvent(Map<String, Object> reservedEventMap) {
		try {
			if (reservedEventMap == null || reservedEventMap.isEmpty())
				return;
			log.info("Order Service consumed InventoryReserved event: {}", reservedEventMap);

			InventoryEventDto dto = objectMapper.convertValue(reservedEventMap, InventoryEventDto.class);
			String orderIdStr = dto.getOrderId();

			if (orderIdStr != null) {
				Long orderId = Long.valueOf(orderIdStr);
				orderRepository.findById(orderId).ifPresent(order -> {
					order.setStatus("INVENTORY_RESERVED");
					orderRepository.save(order);
					log.info("Order {} status updated to INVENTORY_RESERVED", orderId);
				});
			}
		} catch (Exception e) {
			log.error("Error processing InventoryReserved event: {}", e.getMessage(), e);
		}
	}

	@KafkaListener(topics = "${app.kafka.topic.payment-success}", groupId = "order-group")
	@Transactional
	public void consumePaymentSuccessEvent(Map<String, Object> paymentEventMap) {
		try {
			if (paymentEventMap == null || paymentEventMap.isEmpty())
				return;
			log.info("Order Service consumed PaymentSuccess event: {}", paymentEventMap);

			// The payment event publishes the Payment object, which contains 'orderId'
			Number orderIdNumber = (Number) paymentEventMap.get("orderId");
			if (orderIdNumber != null) {
				Long orderId = orderIdNumber.longValue();
				orderRepository.findById(orderId).ifPresent(order -> {
					order.setStatus("APPROVED");
					orderRepository.save(order);
					log.info("Order {} status updated to APPROVED", orderId);
				});
			}
		} catch (Exception e) {
			log.error("Error processing PaymentSuccess event: {}", e.getMessage(), e);
		}
	}

	@KafkaListener(topics = "${app.kafka.topic.inventory-failed}", groupId = "order-group")
	@Transactional
	public void consumeInventoryFailedEvent(Map<String, Object> failedEventMap) {
		try {
			if (failedEventMap == null || failedEventMap.isEmpty())
				return;
			log.info("Order Service consumed InventoryFailed event: {}", failedEventMap);

			InventoryEventDto dto = objectMapper.convertValue(failedEventMap, InventoryEventDto.class);
			String orderIdStr = dto.getOrderId();
			String reason = dto.getReason();

			if (orderIdStr != null) {
				Long orderId = Long.valueOf(orderIdStr);
				orderRepository.findById(orderId).ifPresent(order -> {
					order.setStatus("REJECTED");
					// You can also save the reason if there is a field for it
					orderRepository.save(order);
					log.info("Order {} status updated to REJECTED due to {}", orderId, reason);
				});
			}
		} catch (Exception e) {
			log.error("Error processing InventoryFailed event: {}", e.getMessage(), e);
		}
	}
}
