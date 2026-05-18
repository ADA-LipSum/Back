package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "제재 사유별 통계")
public class BanStatsResponse {

    @Schema(description = "제재 사유")
    private String reason;

    @Schema(description = "해당 사유 제재 횟수")
    private long count;
}
