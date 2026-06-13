package com.ada.proj.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.*;

// 포인트 -> 코인 교환 내역 (월별 한도 계산에 사용)
@Entity
@Table(name = "point_exchange_log",
        indexes = @Index(name = "idx_point_exchange_log_user_created", columnList = "user_uuid, created_at"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointExchangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_uuid", length = 36, nullable = false)
    private String userUuid;

    @Column(name = "points", nullable = false)
    private Integer points;

    @Column(name = "coins", nullable = false)
    private Integer coins;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
