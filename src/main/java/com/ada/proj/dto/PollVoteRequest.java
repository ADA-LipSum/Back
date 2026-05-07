package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "PollVoteRequest", description = "투표 참여 요청")
public class PollVoteRequest {

    @NotNull
    @Schema(description = "선택할 투표 선택지 ID", example = "1")
    private Long optionId;
}
