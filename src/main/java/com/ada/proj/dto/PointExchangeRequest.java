package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "포인트 -> 코인 교환 요청")
public class PointExchangeRequest {

    @NotNull
    @Positive
    @Schema(description = "교환할 포인트 수 (교환 비율의 배수가 아니면 나머지는 차감되지 않고 그대로 남습니다)", example = "100")
    private Integer points;
}
