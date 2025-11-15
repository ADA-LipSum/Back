//C:\Users\russe\Documents\GitHub\Ada\Back\src\main\java\com\ada\proj\dto\TradeItemDetailRequest.java
package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(name = "TradeItemDetailRequest", description = "아이템 상세 조회 요청 바디")
public class TradeItemDetailRequest {

    @NotBlank
    @Schema(description = "조회할 아이템 UUID", example = "1b6c2a5f-0d7a-4d09-9c4a-1f2e3a4b5c6d")
    private String itemUuid;
}