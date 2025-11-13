package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PointsUseRequest {
    @NotBlank
    @Schema(description = "대상 사용자 UUID", example = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
    private String userUuid;

    @Min(1)
    @Schema(description = "사용 포인트(양수)", example = "30")
    private int points;

    @NotBlank
    @Schema(description = "사용처", example = "ITEM_PURCHASE")
    private String usedFor;

    @Schema(description = "사용 상세 메타데이터(JSON 문자열)", example = "{}")
    private String metadata;

    @Schema(description = "사유/메모", example = "아이템 구매")
    private String description;
}
