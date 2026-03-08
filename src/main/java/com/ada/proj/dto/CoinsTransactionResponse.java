package com.ada.proj.dto;

import java.time.Instant;

import com.ada.proj.entity.UserCoins;
import com.ada.proj.enums.PointChangeType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CoinsTransactionResponse {

    @Schema(example = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
    private String coinUuid;
    private String userUuid;
    private PointChangeType changeType;
    private int coins;
    private int balanceAfter;
    private String description;
    private Instant createdAt;

    public static CoinsTransactionResponse from(UserCoins uc) {
        CoinsTransactionResponse r = new CoinsTransactionResponse();
        r.setCoinUuid(uc.getCoinUuid());
        r.setUserUuid(uc.getUserUuid());
        r.setChangeType(uc.getChangeType());
        r.setCoins(uc.getCoins());
        r.setBalanceAfter(uc.getBalanceAfter());
        r.setDescription(uc.getDescription());
        r.setCreatedAt(uc.getCreatedAt());
        return r;
    }
}
