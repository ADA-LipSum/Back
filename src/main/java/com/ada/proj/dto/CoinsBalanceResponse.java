package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CoinsBalanceResponse {

    @Schema(example = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
    private String userUuid;
    @Schema(example = "500")
    private int totalCoins;
}
