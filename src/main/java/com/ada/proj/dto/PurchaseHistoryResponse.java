package com.ada.proj.dto;

import java.time.Instant;

import lombok.Data;

@Data
public class PurchaseHistoryResponse {

    private String itemName;
    private int count = 1;
    private String studentId;
    private Instant purchasedAt;

    public PurchaseHistoryResponse(String itemName, String studentId, Instant purchasedAt) {
        this.itemName = itemName;
        this.studentId = studentId;
        this.purchasedAt = purchasedAt;
    }
}