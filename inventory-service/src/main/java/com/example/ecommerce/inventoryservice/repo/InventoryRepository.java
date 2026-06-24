package com.example.ecommerce.inventoryservice.repo;

import com.example.ecommerce.inventoryservice.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByProductCode(String productCode);
    Optional<Inventory> findByProductCodeAndStatus(String productCode, String status);
    List<Inventory> findAllByStatus(String status);
}
