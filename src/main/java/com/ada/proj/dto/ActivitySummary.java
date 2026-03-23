package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(name = "ActivitySummary", description = "활동 내역 요약")
public class ActivitySummary {

    @Schema(description = "작성한 게시글 수")
    private long postCount;

    @Schema(description = "작성한 댓글 수")
    private long commentCount;
}
