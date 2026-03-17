package com.ada.proj.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cart_item",
        indexes = {
                @Index(name = "idx_cart_item_user_uuid", columnList = "user_uuid"),
                @Index(name = "idx_cart_item_uuid",      columnList = "cart_item_uuid")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cart_item_uuid",  columnNames = {"cart_item_uuid"}),
                @UniqueConstraint(name = "uk_cart_user_item",  columnNames = {"user_uuid", "item_uuid"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cart_item_uuid", length = 36, nullable = false, unique = true)
    private String cartItemUuid;

    @Column(name = "user_uuid", length = 36, nullable = false)
    private String userUuid;

    @Column(name = "item_uuid", length = 36, nullable = false)
    private String itemUuid;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
