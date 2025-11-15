package com.ada.proj.dto;

import java.time.Instant;

import com.ada.proj.entity.PurchaseHistory;

import lombok.Data;

@Data
public class PurchaseItemResponse {

    private String purchaseUuid;
    private String userUuid;
    private String itemUuid;
    private int pointsUsed;
    private String pointsTxUuid;
    private Instant createdAt;

    public static PurchaseItemResponse from(PurchaseHistory ph) {
        PurchaseItemResponse r = new PurchaseItemResponse();
        r.setPurchaseUuid(ph.getPurchaseUuid());
        r.setUserUuid(ph.getUserUuid());
        r.setItemUuid(ph.getItemUuid());
        r.setPointsUsed(ph.getPointsUsed());
        r.setPointsTxUuid(ph.getPointsTxUuid());
        r.setCreatedAt(ph.getCreatedAt());
        return r;
    }
}