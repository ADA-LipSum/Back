package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "포인트 -> 코인 교환 결과")
public class PointExchangeResponse {

    @Schema(description = "이번에 차감된 포인트 (교환 비율의 배수)", example = "100")
    private int usedPoints;

    @Schema(description = "이번에 지급된 코인", example = "1")
    private int receivedCoins;

    @Schema(description = "적용된 교환 비율 (코인 1개당 포인트)", example = "100")
    private int pointsPerCoin;

    @Schema(description = "교환 후 포인트 잔액")
    private int pointsBalance;

    @Schema(description = "교환 후 코인 잔액")
    private int coinsBalance;

    @Schema(description = "1인당 월 최대 교환 가능 코인 수", example = "100")
    private int monthlyCoinLimit;

    @Schema(description = "이번 달 누적 교환 코인 수 (이번 교환 포함)")
    private int monthlyUsedCoins;

    @Schema(description = "이번 달 남은 교환 가능 코인 수")
    private int monthlyRemainingCoins;
}
