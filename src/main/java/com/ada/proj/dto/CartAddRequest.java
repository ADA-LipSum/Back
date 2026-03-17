package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(name = "CartAddRequest", description = "카트 아이템 추가 요청")
public class CartAddRequest {

    @NotBlank
    @Schema(description = "추가할 아이템 UUID", example = "1b6c2a5f-0d7a-4d09-9c4a-1f2e3a4b5c6d")
    private String itemUuid;

    @Min(1)
    @Schema(description = "추가할 수량 (최소 1)", example = "1")
    private int quantity = 1;
}
