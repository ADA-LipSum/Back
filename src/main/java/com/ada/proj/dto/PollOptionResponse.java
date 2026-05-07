package com.ada.proj.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PollOptionResponse", description = "투표 선택지 응답")
public class PollOptionResponse {

    @Schema(description = "선택지 ID", example = "1")
    private Long id;

    @Schema(description = "선택지 내용", example = "MySQL")
    private String text;

    @Schema(description = "선택지 득표 수", example = "7")
    private int voteCount;

    @Schema(description = "투표자 목록. 익명 투표이면 빈 배열입니다.")
    private List<PollVoterResponse> voters;
}
