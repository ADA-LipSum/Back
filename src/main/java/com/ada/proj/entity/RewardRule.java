package com.ada.proj.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.ada.proj.enums.RewardActionCode;
import com.ada.proj.enums.RewardLimitType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reward_rule",
        uniqueConstraints = @UniqueConstraint(name = "uk_reward_rule_action_code", columnNames = {"action_code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_code", length = 50, nullable = false)
    private RewardActionCode actionCode;

    @Column(name = "action_name", length = 100, nullable = false)
    private String actionName;

    @Column(name = "points", nullable = false)
    private Integer points;

    @Column(name = "coins", nullable = false)
    private Integer coins;

    @Enumerated(EnumType.STRING)
    @Column(name = "limit_type", length = 20, nullable = false)
    private RewardLimitType limitType;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "description", length = 255)
    private String description;

    // POST_VISIT_MILESTONE처럼 "N개마다" 같은 횟수 기준이 필요한 행동에서만 사용 (그 외 null)
    @Column(name = "threshold")
    private Integer threshold;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
