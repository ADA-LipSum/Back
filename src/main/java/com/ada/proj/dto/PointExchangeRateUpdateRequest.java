package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "포인트 -> 코인 교환 비율/한도 수정 요청 (값을 입력한 필드만 변경됩니다)")
public class PointExchangeRateUpdateRequest {

    @Positive
    @Schema(description = "코인 1개로 교환하는 데 필요한 포인트 수", example = "100")
    private Integer pointsPerCoin;

    @Positive
    @Schema(description = "1인당 월 최대 교환 가능 코인 수", example = "100")
    private Integer monthlyCoinLimit;
}
