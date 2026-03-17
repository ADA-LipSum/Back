package com.ada.proj.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CartCheckoutResponse", description = "카트 일괄 결제 응답")
public class CartCheckoutResponse {

    @Schema(description = "구매된 아이템 종류 수")
    private int itemCount;

    @Schema(description = "총 차감 코인")
    private int totalCoins;

    @Schema(description = "결제 후 코인 잔액")
    private int balanceAfter;

    @Schema(description = "아이템별 거래 결과")
    private List<CartItemResponse> items;
}
