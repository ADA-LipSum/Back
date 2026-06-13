package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "포인트 -> 코인 교환 비율")
public class PointExchangeRateResponse {

    @Schema(description = "코인 1개로 교환하는 데 필요한 포인트 수", example = "100")
    private int pointsPerCoin;

    @Schema(description = "1인당 월 최대 교환 가능 코인 수", example = "100")
    private int monthlyCoinLimit;
}
