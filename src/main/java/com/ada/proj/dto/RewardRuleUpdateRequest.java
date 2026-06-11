package com.ada.proj.dto;

import com.ada.proj.enums.RewardLimitType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
@Schema(description = "행동 보상 규칙 수정 요청 (값을 입력한 필드만 변경됩니다)")
public class RewardRuleUpdateRequest {

    @PositiveOrZero
    @Schema(description = "지급 포인트", example = "10")
    private Integer points;

    @PositiveOrZero
    @Schema(description = "지급 코인", example = "5")
    private Integer coins;

    @Schema(description = "지급 제한 (NONE: 제한 없음 | DAILY: 1일 1회 | ONCE: 평생 1회 | ONCE_PER_TARGET: 대상별 1회)")
    private RewardLimitType limitType;

    @Schema(description = "활성화 여부")
    private Boolean enabled;

    @Schema(description = "설명")
    private String description;

    @Positive
    @Schema(description = "지급 기준 횟수 (POST_VISIT_MILESTONE처럼 'N개마다' 지급되는 행동에서만 사용)", example = "10")
    private Integer threshold;
}
