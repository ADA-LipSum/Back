package com.ada.proj.entity;

import java.time.Instant;

import com.ada.proj.enums.PointChangeType;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_coins",
        indexes = {
            @Index(name = "idx_user_coins_user_uuid", columnList = "user_uuid"),
            @Index(name = "idx_user_coins_created_at", columnList = "created_at")
        },
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_user_coins_coin_uuid", columnNames = {"coin_uuid"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCoins {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    @Column(name = "coin_uuid", length = 36, nullable = false, unique = true)
    private String coinUuid;

    @Column(name = "user_uuid", length = 36, nullable = false)
    private String userUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", length = 10, nullable = false)
    private PointChangeType changeType;

    // 변화값 (+/-). GAIN은 양수, LOSS/USE는 음수
    @Column(name = "coins", nullable = false)
    private Integer coins;

    // 반영 후 잔액 스냅샷
    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Column(name = "description", length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
