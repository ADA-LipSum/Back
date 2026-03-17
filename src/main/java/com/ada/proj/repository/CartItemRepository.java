package com.ada.proj.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ada.proj.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserUuidOrderByCreatedAtAsc(String userUuid);

    Optional<CartItem> findByCartItemUuid(String cartItemUuid);

    Optional<CartItem> findByUserUuidAndItemUuid(String userUuid, String itemUuid);

    void deleteByUserUuid(String userUuid);
}
