package com.ada.proj.dto;

import com.ada.proj.entity.TradeItem;
import com.ada.proj.entity.TradeLog;
import com.ada.proj.entity.UserPoints;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TradePurchaseResponse", description = "아이템 구매 응답")
public class TradePurchaseResponse {
    @Schema(description = "거래 로그 UUID", example = "4f3d8a1e-8b8b-4c3b-9a2c-7f5a2d1c0b9e")
    private String logUuid;

    @Schema(description = "아이템 UUID", example = "1b6c2a5f-0d7a-4d09-9c4a-1f2e3a4b5c6d")
    private String itemUuid;

    @Schema(description = "아이템 이름", example = "프리미엄 배지")
    private String itemName;

    @Schema(description = "구매 수량", example = "2")
    private int quantity;

    @Schema(description = "단가(포인트)", example = "100")
    private int unitPrice;

    @Schema(description = "총 차감 포인트", example = "200")
    private int totalPoints;

    @Schema(description = "포인트 트랜잭션 UUID (USE)", example = "c1a2b3d4-e5f6-7890-a1b2-c3d4e5f67890")
    private String pointsUuid;

    @Schema(description = "구매 후 잔여 포인트", example = "850")
    private int balanceAfter;

    public static TradePurchaseResponse of(TradeItem item, TradeLog log, UserPoints pointsTx) {
        return TradePurchaseResponse.builder()
                .logUuid(log.getLogUuid())
                .itemUuid(item.getItemUuid())
                .itemName(item.getName())
                .quantity(log.getQuantity())
                .unitPrice(item.getPrice())
                .totalPoints(log.getTotalPoints())
                .pointsUuid(pointsTx != null ? pointsTx.getPointsUuid() : null)
                .balanceAfter(pointsTx != null ? pointsTx.getBalanceAfter() : 0)
                .build();
    }
}
