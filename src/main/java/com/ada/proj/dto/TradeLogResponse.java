package com.ada.proj.dto;

import java.time.Instant;

import com.ada.proj.entity.TradeLog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TradeLogResponse", description = "구매내역 항목")
public class TradeLogResponse {
    @Schema(description = "거래 로그 UUID", example = "4f3d8a1e-8b8b-4c3b-9a2c-7f5a2d1c0b9e")
    private String logUuid;

    @Schema(description = "아이템 UUID", example = "1b6c2a5f-0d7a-4d09-9c4a-1f2e3a4b5c6d")
    private String itemUuid;

    @Schema(description = "아이템 이름", example = "프리미엄 배지")
    private String itemName;

    @Schema(description = "구매 수량", example = "1")
    private Integer quantity;

    @Schema(description = "단가(포인트)", example = "100")
    private Integer unitPrice;

    @Schema(description = "총 포인트", example = "100")
    private Integer totalPoints;

    @Schema(description = "연결된 포인트 트랜잭션 UUID", example = "c1a2b3d4-e5f6-7890-a1b2-c3d4e5f67890")
    private String pointsUuid;

    @Schema(description = "기록 시각", example = "2025-11-16T01:53:26Z")
    private Instant createdAt;

    public static TradeLogResponse from(TradeLog l) {
        return TradeLogResponse.builder()
                .logUuid(l.getLogUuid())
                .itemUuid(l.getItemUuid())
                .itemName(l.getItemName())
                .quantity(l.getQuantity())
                .unitPrice(l.getUnitPrice())
                .totalPoints(l.getTotalPoints())
                .pointsUuid(l.getPointsUuid())
                .createdAt(l.getCreatedAt())
                .build();
    }
}
