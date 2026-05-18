package com.ada.proj.dto;

import com.ada.proj.enums.TradeCurrency;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "구매 취소/환불 응답")
public class TradeOrderCancelResponse {

    @Schema(description = "취소된 주문 UUID")
    private String logUuid;

    @Schema(description = "아이템 이름")
    private String itemName;

    @Schema(description = "취소된 수량")
    private int quantity;

    @Schema(description = "환불된 금액")
    private int refundAmount;

    @Schema(description = "결제 수단 (COIN/POINT)")
    private TradeCurrency currency;

    @Schema(description = "환불 후 잔액")
    private int balanceAfter;
}
