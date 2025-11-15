package com.ada.proj.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PointsTransaction {
    private final String pointsUuid;
    private final String userUuid;
    private final int amount;
    private final String reasonCode;
    private final String refUuid;
    private final String description;
    private final Instant createdAt;

    public static PointsTransaction create(String userUuid, int amount, String reasonCode, String refUuid, String description) {
        return PointsTransaction.builder()
                .pointsUuid(UUID.randomUUID().toString())
                .userUuid(userUuid)
                .amount(amount)
                .reasonCode(reasonCode)
                .refUuid(refUuid)
                .description(description)
                .createdAt(Instant.now())
                .build();
    }
}
