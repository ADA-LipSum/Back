package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "구매 내역 통계")
public class TradeOrderStatsResponse {

    @Schema(description = "전체 주문 수")
    private long totalOrders;

    @Schema(description = "취소된 주문 수")
    private long cancelledOrders;

    @Schema(description = "활성 주문 수")
    private long activeOrders;

    @Schema(description = "코인 결제 총액")
    private long totalCoinsSpent;

    @Schema(description = "포인트 결제 총액")
    private long totalPointsSpent;
}
