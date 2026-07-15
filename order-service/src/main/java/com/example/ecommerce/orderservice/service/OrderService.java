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

import com.example.ecommerce.orderservice.client.InventoryClient;
import com.example.ecommerce.orderservice.client.UserClient;
import com.example.ecommerce.orderservice.dto.InventoryDto;
import com.example.ecommerce.orderservice.dto.InventoryEventDto;
import com.example.ecommerce.orderservice.dto.UserDto;
import com.example.ecommerce.orderservice.entity.Order;
import com.example.ecommerce.orderservice.repo.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RefreshScope
public class OrderService {

	private final OrderRepository orderRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	private final UserClient userClient;
	private final InventoryClient inventoryClient;
	private final ObjectMapper objectMapper;

	@Value("${app.kafka.topic.order-created}")
	private String orderCreatedTopic;

	public OrderService(OrderRepository orderRepository, KafkaTemplate<String, Object> kafkaTemplate,
			UserClient userClient, ObjectMapper objectMapper, InventoryClient inventoryClient) {
		this.orderRepository = orderRepository;
		this.kafkaTemplate = kafkaTemplate;

		this.userClient = userClient;
		this.objectMapper = objectMapper;
		this.inventoryClient = inventoryClient;
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
				log.warn("User validation failed: User with ID {} does not exist", order.getUserId());
				order.setStatus("REJECTED_USER_NOT_FOUND");
				return order;
			}
		} catch (Exception e) {
			log.error("User validation failed due to service error: {}", e.getMessage());
			order.setStatus("REJECTED_USER_SERVICE_ERROR");
			return order;
		}

		// Synchronous Product Validation via OpenFeign
		try {
			InventoryDto inventoryDto = inventoryClient.getInventoryByProductCode(order.getProductCode());
			if (inventoryDto == null || inventoryDto.getId() == null) {
				log.warn("Product validation failed: Product code {} does not exist", order.getProductCode());
				order.setStatus("REJECTED_PRODUCT_NOT_FOUND");
				return order;
			}
			if ("INACTIVE".equalsIgnoreCase(inventoryDto.getStatus())) {
				log.warn("Product validation failed: Product code {} is INACTIVE", order.getProductCode());
				order.setStatus("REJECTED_PRODUCT_INACTIVE");
				return order;
			}
		} catch (Exception e) {
			log.error("Product validation failed due to service error: {}", e.getMessage());
			order.setStatus("REJECTED_INVENTORY_SERVICE_ERROR");
			return order;
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

	public List<Order> getOrderById(String userId) {
		if (userId != null) {
			List<Order> orders = orderRepository.findByUserId(userId);
			if (orders != null && !orders.isEmpty()) {
				return orders;
			} else {
				log.error("No orders found for UserId {}", userId);
				return List.of();
			}
		} else {
			log.error("userId is null {}", userId);
		}
		return List.of();
	}

	public Order getOrderByOrderId(Long orderId) {
		if (orderId != null) {
			return orderRepository.findById(orderId).orElse(null);
		} else {
			log.error("orderId is null");
		}
		return null;
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
