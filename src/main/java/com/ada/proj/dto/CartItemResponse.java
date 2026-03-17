package com.ada.proj.dto;

import java.time.Instant;

import com.ada.proj.entity.CartItem;
import com.ada.proj.entity.TradeItem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CartItemResponse", description = "카트 아이템 응답")
public class CartItemResponse {

    @Schema(description = "카트 아이템 UUID")
    private String cartItemUuid;

    @Schema(description = "아이템 UUID")
    private String itemUuid;

    @Schema(description = "아이템 이름")
    private String itemName;

    @Schema(description = "아이템 이미지 URL")
    private String imageUrl;

    @Schema(description = "단가(코인)")
    private Integer unitPrice;

    @Schema(description = "수량")
    private Integer quantity;

    @Schema(description = "합계(단가 × 수량)")
    private Integer lineTotal;

    @Schema(description = "아이템 활성화 여부 (false면 결제 불가)")
    private Boolean itemActive;

    @Schema(description = "카트에 추가한 시각")
    private Instant addedAt;

    public static CartItemResponse of(CartItem cart, TradeItem item) {
        return CartItemResponse.builder()
                .cartItemUuid(cart.getCartItemUuid())
                .itemUuid(item.getItemUuid())
                .itemName(item.getName())
                .imageUrl(item.getImageUrl())
                .unitPrice(item.getPrice())
                .quantity(cart.getQuantity())
                .lineTotal(item.getPrice() * cart.getQuantity())
                .itemActive(item.getActive())
                .addedAt(cart.getCreatedAt())
                .build();
    }
}
