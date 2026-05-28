package com.ada.proj.dto;

import com.ada.proj.entity.CustomSticker;
import com.ada.proj.enums.CustomStickerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@Schema(name = "CustomStickerResponse", description = "커스텀 스티커 응답")
public class CustomStickerResponse {

    private String stickerUuid;
    private String userUuid;
    private String name;
    private String description;
    private String imageUrl;
    private CustomStickerStatus status;
    private Integer submissionFee;
    private String reviewedBy;
    private String rejectionReason;
    private Instant reviewedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static CustomStickerResponse from(CustomSticker sticker) {
        return CustomStickerResponse.builder()
                .stickerUuid(sticker.getStickerUuid())
                .userUuid(sticker.getUserUuid())
                .name(sticker.getName())
                .description(sticker.getDescription())
                .imageUrl(sticker.getImageUrl())
                .status(sticker.getStatus())
                .submissionFee(sticker.getSubmissionFee())
                .reviewedBy(sticker.getReviewedBy())
                .rejectionReason(sticker.getRejectionReason())
                .reviewedAt(sticker.getReviewedAt())
                .createdAt(sticker.getCreatedAt())
                .updatedAt(sticker.getUpdatedAt())
                .build();
    }
}
