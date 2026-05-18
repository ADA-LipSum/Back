package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "아이템별 판매 통계")
public class TradeItemStatsResponse {

    @Schema(description = "아이템 UUID")
    private String itemUuid;

    @Schema(description = "아이템 이름")
    private String itemName;

    @Schema(description = "총 판매 수량")
    private long totalQuantitySold;

    @Schema(description = "총 매출 (코인/포인트)")
    private long totalRevenue;

    @Schema(description = "주문 건수")
    private long orderCount;
}
