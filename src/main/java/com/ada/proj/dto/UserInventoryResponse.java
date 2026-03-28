package com.ada.proj.dto;

import java.time.Instant;

import com.ada.proj.enums.TradeCategory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(name = "UserInventoryResponse", description = "사용자 인벤토리 아이템 응답")
public class UserInventoryResponse {

    @Schema(description = "인벤토리 UUID", example = "a1b2c3d4-...")
    private String inventoryUuid;

    @Schema(description = "아이템 UUID", example = "e5f6g7h8-...")
    private String itemUuid;

    @Schema(description = "아이템 이름", example = "봄 배너")
    private String itemName;

    @Schema(description = "아이템 이미지 URL (배너 적용 시 사용)", example = "https://bucket.s3.region.amazonaws.com/banners/...")
    private String imageUrl;

    @Schema(description = "카테고리", example = "ETC")
    private TradeCategory category;

    @Schema(description = "소분류", example = "BANNER")
    private TradeCategory subCategory;

    @Schema(description = "획득 일시")
    private Instant acquiredAt;
}
