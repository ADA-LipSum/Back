package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PointsGrantRequest {
    @NotBlank
    @Schema(description = "대상 사용자 UUID", example = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
    private String userUuid;

    @Min(1)
    @Schema(description = "지급 포인트(양수)", example = "100")
    private int points;

    @Schema(description = "사유/메모", example = "신규 가입 보너스")
    private String description;

    @Schema(description = "참조 룰 ID", example = "1")
    private Long refRuleId;
}
