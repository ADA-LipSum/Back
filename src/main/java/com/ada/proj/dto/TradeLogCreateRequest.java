package com.ada.proj.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "TradeLogCreateRequest", description = "수동 거래 로그 생성 요청")
public class TradeLogCreateRequest {
    @NotBlank
    @Schema(description = "아이템 UUID", example = "1b6c2a5f-0d7a-4d09-9c4a-1f2e3a4b5c6d")
    private String itemUuid;

    @Min(1)
    @Schema(description = "수량(최소 1)", example = "1")
    private int quantity;

    @Min(0)
    @Schema(description = "총 포인트(옵션, 미전달 시 단가*수량)", example = "100")
    private int totalPoints; // 수동 로그 시 기록용

    @Schema(description = "아이템 이름(옵션, 기본 DB값)", example = "프리미엄 배지")
    private String itemName; // 필요 시 덮어쓰기

    @Schema(description = "연결할 포인트 트랜잭션 UUID(옵션)", example = "c1a2b3d4-e5f6-7890-a1b2-c3d4e5f67890")
    private String pointsUuid; // 기존 포인트 트랜잭션과 연결하고 싶다면 전달

    @Schema(description = "추가 메타데이터(JSON 문자열)", example = "{\"note\":\"manual\"}")
    private String metadata;
}
