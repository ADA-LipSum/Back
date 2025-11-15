package com.ada.proj.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trade_log",
        indexes = {
                @Index(name = "idx_trade_log_user", columnList = "user_uuid"),
                @Index(name = "idx_trade_log_item", columnList = "item_uuid"),
                @Index(name = "idx_trade_log_created", columnList = "created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeLog {

    @Id
    @Column(name = "log_uuid", length = 36)
    private String logUuid; // assigned

    @Column(name = "user_uuid", length = 36, nullable = false)
    private String userUuid;

    @Column(name = "item_uuid", length = 36, nullable = false)
    private String itemUuid;

    @Column(name = "item_name", length = 100, nullable = false)
    private String itemName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

    @Column(name = "total_points", nullable = false)
    private Integer totalPoints;

    @Column(name = "points_uuid", length = 36)
    private String pointsUuid; // 포인트 트랜잭션 uuid (USE)

    @Column(name = "metadata", columnDefinition = "json")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
