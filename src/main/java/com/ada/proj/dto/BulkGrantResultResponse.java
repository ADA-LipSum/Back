package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "일괄 지급 결과")
public class BulkGrantResultResponse {

    @Schema(description = "대상 인원 수")
    private int totalTargets;

    @Schema(description = "성공 처리 수")
    private int successCount;
}
