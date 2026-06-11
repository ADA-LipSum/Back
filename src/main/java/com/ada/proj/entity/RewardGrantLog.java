package com.ada.proj.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.ada.proj.enums.RewardActionCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reward_grant_log",
        indexes = {
                @Index(name = "idx_reward_grant_log_user_action_created", columnList = "user_uuid, action_code, created_at"),
                @Index(name = "idx_reward_grant_log_user_action_target", columnList = "user_uuid, action_code, target_key")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardGrantLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_uuid", length = 36, nullable = false)
    private String userUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_code", length = 50, nullable = false)
    private RewardActionCode actionCode;

    @Column(name = "points", nullable = false)
    private Integer points;

    @Column(name = "coins", nullable = false)
    private Integer coins;

    // ONCE_PER_TARGET 제한에서 "대상"을 구분하기 위한 키 (예: 방명록 대상 프로필 uuid, 프로젝트 id). 그 외 null.
    @Column(name = "target_key", length = 100)
    private String targetKey;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
