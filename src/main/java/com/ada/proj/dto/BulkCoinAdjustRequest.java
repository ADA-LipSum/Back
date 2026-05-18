package com.ada.proj.dto;

import com.ada.proj.enums.PointChangeType;
import com.ada.proj.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "역할별 코인 일괄 지급/차감 요청")
public class BulkCoinAdjustRequest {

    @NotNull
    @Schema(description = "대상 역할", example = "STUDENT")
    private Role role;

    @NotNull
    @Schema(description = "조정 유형 (GAIN: 지급 | LOSS: 차감)", example = "GAIN")
    private PointChangeType type;

    @Min(1)
    @Schema(description = "코인 수량 (최소 1)", example = "100")
    private int coins;

    @Schema(description = "지급/차감 사유", example = "학기 시작 코인 지급")
    private String description;
}
