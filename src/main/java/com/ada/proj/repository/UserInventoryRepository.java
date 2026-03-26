package com.ada.proj.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ada.proj.entity.UserInventory;

public interface UserInventoryRepository extends JpaRepository<UserInventory, Long> {
    List<UserInventory> findByUserUuidOrderByAcquiredAtDesc(String userUuid);
    Optional<UserInventory> findByInventoryUuid(String inventoryUuid);
    boolean existsByUserUuidAndItemUuid(String userUuid, String itemUuid);
}
