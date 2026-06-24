package com.example.ecommerce.inventoryservice.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ecommerce.inventoryservice.dto.InventoryEventDto;
import com.example.ecommerce.inventoryservice.dto.OrderEventDto;
import com.example.ecommerce.inventoryservice.entity.Inventory;
import com.example.ecommerce.inventoryservice.repo.InventoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topic.inventory-reserved}")
    private String inventoryReservedTopic;

    @Value("${app.kafka.topic.inventory-failed}")
    private String inventoryFailedTopic;

    
    public InventoryService(InventoryRepository inventoryRepository, KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.inventoryRepository = inventoryRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @CachePut(value = "inventoryCache", key = "#result.productCode")
    @CacheEvict(value = "inventoryListCache", allEntries = true)
    public Inventory addStock(Inventory inventory) {
        inventory.setStatus("ACTIVE");
        return inventoryRepository.findByProductCode(inventory.getProductCode())
                .map(existing -> {
                    existing.setStock(existing.getStock() + inventory.getStock());
                    existing.setStatus("ACTIVE");
                    return inventoryRepository.save(existing);
                })
                .orElseGet(() -> inventoryRepository.save(inventory));
    }

    @Cacheable(value = "inventoryListCache", key = "'allActive'")
    public List<Inventory> getAllInventory() {
        return inventoryRepository.findAllByStatus("ACTIVE");
    }

    @Cacheable(value = "inventoryCache", key = "#productCode")
    public java.util.Optional<Inventory> getByProductCode(String productCode) {
        return inventoryRepository.findByProductCodeAndStatus(productCode, "ACTIVE");
    }

    @KafkaListener(topics = "${app.kafka.topic.order-created}", groupId = "inventory-group")
    @Transactional
    @CacheEvict(value = {"inventoryCache", "inventoryListCache"}, allEntries = true)
    public void consumeOrderCreatedEvent(Map<String, Object> orderEvent) {
        try {
            if (orderEvent == null || orderEvent.isEmpty()) return;
            log.info("Inventory Service consumed OrderCreated event: {}", orderEvent);
            
            OrderEventDto dto = objectMapper.convertValue(orderEvent, OrderEventDto.class);
            String productCode = dto.getProductCode();
            Integer quantity = dto.getQuantity();
            String orderId = dto.getId();

            if (productCode != null && quantity != null && orderId != null) {
                inventoryRepository.findByProductCodeAndStatus(productCode, "ACTIVE").ifPresentOrElse(inventory -> {
                    if (inventory.getStock() >= quantity) {
                        inventory.setStock(inventory.getStock() - quantity);
                        inventoryRepository.save(inventory);
                        log.info("Decremented stock for product: {} by {}", productCode, quantity);
                        
                        // Publish success event
                        InventoryEventDto reservedEvent = new InventoryEventDto(orderId, productCode, "RESERVED", null);
                        kafkaTemplate.send(inventoryReservedTopic, reservedEvent);
                    } else {
                        log.warn("Insufficient stock for product: {}", productCode);
                        // Publish failure event
                        InventoryEventDto failedEvent = new InventoryEventDto(orderId, productCode, "FAILED", "INSUFFICIENT_STOCK");
                        kafkaTemplate.send(inventoryFailedTopic, failedEvent);
                    }
                }, () -> {
                    log.warn("Product not found in active inventory: {}", productCode);
                    // Publish failure event
                    InventoryEventDto failedEvent = new InventoryEventDto(orderId, productCode, "FAILED", "PRODUCT_NOT_FOUND");
                    kafkaTemplate.send(inventoryFailedTopic, failedEvent);
                });
            } else {
                log.warn("Order event missing required fields (orderId, productCode, quantity)");
            }
        } catch (Exception e) {
            log.error("Error processing OrderCreated event: {}", e.getMessage(), e);
        }
    }

    @CacheEvict(value = {"inventoryCache", "inventoryListCache"}, allEntries = true)
    public void refreshCache() {
        log.info("Inventory caches refreshed manually.");
    }
}
