package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "거래 아이템 수정 요청 (포함된 필드만 업데이트)")
public class TradeItemUpdateRequest {

    @Schema(description = "아이템 이름", example = "새 간식")
    private String name;

    @Schema(description = "아이템 설명", example = "수정된 설명")
    private String description;

    @Schema(description = "가격 (최소 0)", example = "150")
    private Integer price;

    @Schema(description = "판매 활성화 여부", example = "true")
    private Boolean active;

    @Schema(description = "이미지 URL", example = "https://example.com/image.png")
    private String imageUrl;
}
