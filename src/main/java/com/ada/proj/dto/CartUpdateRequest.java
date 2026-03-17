package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(name = "CartUpdateRequest", description = "카트 수량 변경 요청")
public class CartUpdateRequest {

    @Min(1)
    @Schema(description = "변경할 수량 (최소 1, 0으로 줄이려면 DELETE 사용)", example = "3")
    private int quantity;
}
