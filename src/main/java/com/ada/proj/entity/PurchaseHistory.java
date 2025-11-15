package com.ada.proj.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "purchase_history",
        indexes = {
                @Index(name = "idx_purchase_user_uuid", columnList = "user_uuid"),
                @Index(name = "idx_purchase_item_uuid", columnList = "item_uuid")
        })
public class PurchaseHistory {

    @Id
    @Column(name = "purchase_uuid", length = 36, nullable = false)
    private String purchaseUuid;

    @Column(name = "user_uuid", length = 36, nullable = false)
    private String userUuid;

    @Column(name = "item_uuid", length = 36, nullable = false)
    private String itemUuid;

    @Column(name = "points_used", nullable = false)
    private Integer pointsUsed;

    @Column(name = "points_tx_uuid", length = 36)
    private String pointsTxUuid; // UserPoints.pointsUuid

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (this.purchaseUuid == null) {
            this.purchaseUuid = UUID.randomUUID().toString();
        }
    }
}