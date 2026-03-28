package com.ada.proj.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_inventory",
        indexes = {
                @Index(name = "idx_user_inventory_user_uuid", columnList = "user_uuid"),
                @Index(name = "idx_user_inventory_item_uuid", columnList = "item_uuid")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_inventory_uuid", columnNames = {"inventory_uuid"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_uuid", length = 36, nullable = false, unique = true)
    private String inventoryUuid;

    @Column(name = "user_uuid", length = 36, nullable = false)
    private String userUuid;

    @Column(name = "item_uuid", length = 36, nullable = false)
    private String itemUuid;

    @CreationTimestamp
    @Column(name = "acquired_at", updatable = false, nullable = false)
    private Instant acquiredAt;
}
