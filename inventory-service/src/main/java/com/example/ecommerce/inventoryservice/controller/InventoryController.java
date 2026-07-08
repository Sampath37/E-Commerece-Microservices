package com.example.ecommerce.inventoryservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.inventoryservice.entity.Inventory;
import com.example.ecommerce.inventoryservice.service.InventoryService;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

	private final InventoryService inventoryService;

	public InventoryController(InventoryService inventoryService) {
		this.inventoryService = inventoryService;
	}

	@PostMapping
	public ResponseEntity<Inventory> addStock(@RequestBody Inventory inventory) {
		return ResponseEntity.ok(inventoryService.addStock(inventory));
	}

	@PutMapping("/{productCode}/stock")
	public ResponseEntity<Inventory> updateStock(@PathVariable String productCode, @RequestParam Integer stock) {
		return ResponseEntity.ok(inventoryService.updateStock(productCode, stock));
	}

	@GetMapping
	public ResponseEntity<List<Inventory>> getAllInventory() {
		return ResponseEntity.ok(inventoryService.getAllInventory());
	}

	@GetMapping("/product/{productCode}")
	public ResponseEntity<Inventory> getInventoryByProductCode(@PathVariable String productCode) {
		return inventoryService.getByProductCode(productCode).map(ResponseEntity::ok).orElseThrow(
				() -> new ResourceNotFoundException("Inventory not found for product code: " + productCode));
	}

	@PostMapping("/cache/refresh")
	public ResponseEntity<String> refreshCache() {
		inventoryService.refreshCache();
		return ResponseEntity.ok("Cache refreshed successfully.");
	}
}
