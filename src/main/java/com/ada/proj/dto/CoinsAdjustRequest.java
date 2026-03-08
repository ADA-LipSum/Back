package com.ada.proj.dto;

import com.ada.proj.enums.PointChangeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "코인 조정 요청")
public class CoinsAdjustRequest {

    @NotBlank
    @Schema(description = "대상 사용자 UUID", example = "user-uuid-1234")
    private String userUuid;

    @NotNull
    @Schema(description = "조정 유형 (GAIN/LOSS/USE)", example = "GAIN")
    private PointChangeType type;

    @Min(1)
    @Schema(description = "코인 수량", example = "100")
    private int coins;

    @Schema(description = "설명", example = "간식 구매를 위한 코인 지급")
    private String description;
}
