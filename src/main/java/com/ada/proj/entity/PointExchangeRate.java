package com.ada.proj.entity;

import java.time.Instant;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.*;

// 포인트 -> 코인 교환 비율 설정 (단일 행, id=1로 고정)
@Entity
@Table(name = "point_exchange_rate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointExchangeRate {

    @Id
    private Long id;

    // 코인 1개로 교환하는 데 필요한 포인트 수
    @Column(name = "points_per_coin", nullable = false)
    private Integer pointsPerCoin;

    // 1인당 월(月) 최대 교환 가능 코인 수
    @Column(name = "monthly_coin_limit", nullable = false)
    private Integer monthlyCoinLimit;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
