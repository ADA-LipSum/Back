package com.ada.proj.dto;

import java.time.Instant;

import com.ada.proj.entity.RewardRule;
import com.ada.proj.enums.RewardActionCode;
import com.ada.proj.enums.RewardLimitType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "행동 보상(포인트/코인) 규칙")
public class RewardRuleResponse {

    @Schema(description = "규칙 ID")
    private Long id;

    @Schema(description = "행동 코드", example = "LOGIN_FIRST")
    private RewardActionCode actionCode;

    @Schema(description = "행동 이름", example = "최초 로그인")
    private String actionName;

    @Schema(description = "지급 포인트", example = "10")
    private Integer points;

    @Schema(description = "지급 코인", example = "5")
    private Integer coins;

    @Schema(description = "지급 제한 (NONE: 제한 없음 | DAILY: 1일 1회 | ONCE: 평생 1회 | ONCE_PER_TARGET: 대상별 1회)")
    private RewardLimitType limitType;

    @Schema(description = "활성화 여부")
    private Boolean enabled;

    @Schema(description = "설명")
    private String description;

    @Schema(description = "지급 기준 횟수 (POST_VISIT_MILESTONE처럼 'N개마다' 지급되는 행동에서만 사용, 그 외 null)")
    private Integer threshold;

    private Instant createdAt;
    private Instant updatedAt;

    public static RewardRuleResponse from(RewardRule rule) {
        return RewardRuleResponse.builder()
                .id(rule.getId())
                .actionCode(rule.getActionCode())
                .actionName(rule.getActionName())
                .points(rule.getPoints())
                .coins(rule.getCoins())
                .limitType(rule.getLimitType())
                .enabled(rule.getEnabled())
                .description(rule.getDescription())
                .threshold(rule.getThreshold())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}
