package com.ada.proj.dto;

import com.ada.proj.enums.CustomStickerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "CustomStickerReviewRequest", description = "커스텀 스티커 검수 요청 (ADMIN/TEACHER)")
public class CustomStickerReviewRequest {

    @NotNull
    @Schema(description = "검수 결과 (APPROVED 또는 REJECTED)", example = "APPROVED")
    private CustomStickerStatus status;

    @Schema(description = "반려 사유 (status=REJECTED 일 때 필수)", example = "저작권 침해 가능성이 있는 이미지입니다.")
    private String rejectionReason;

    @Schema(description = "반려 시 포인트 환불 여부 (기본값 true)", example = "true", defaultValue = "true")
    private Boolean refundOnReject = true;
}
