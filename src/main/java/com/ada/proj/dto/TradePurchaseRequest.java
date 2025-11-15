package com.ada.proj.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "TradePurchaseRequest", description = "아이템 구매 요청 바디")
public class TradePurchaseRequest {
    @NotBlank
    @Schema(description = "구매할 아이템의 UUID", example = "1b6c2a5f-0d7a-4d09-9c4a-1f2e3a4b5c6d")
    private String itemUuid;

    @Min(1)
    @Schema(description = "구매 수량(최소 1)", example = "2")
    private int quantity = 1;

    @Schema(description = "추가 메타데이터(JSON 문자열) - 예: 색상/옵션 등", example = "{\"color\":\"gold\"}")
    private String metadata; // 구매 추가 정보(json string)
}
