package com.ada.proj.dto;

import java.time.LocalDateTime;
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
@Schema(name = "PollResponse", description = "투표 상세 응답")
public class PollResponse {

    @Schema(description = "투표 ID", example = "1")
    private Long id;

    @Schema(description = "투표가 연결된 게시글 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String postUuid;

    @Schema(description = "투표 질문", example = "다음 프로젝트 DB는 무엇으로 할까요?")
    private String question;

    @Schema(description = "익명 투표 여부", example = "false")
    private boolean anonymous;

    @Schema(description = "투표 종료 시각", example = "2026-05-08T18:00:00")
    private LocalDateTime endsAt;

    @Schema(description = "투표 종료 여부", example = "false")
    private boolean ended;

    @Schema(description = "총 투표 수", example = "12")
    private int totalVotes;

    @Schema(description = "로그인 사용자가 선택한 선택지 ID. 비로그인 또는 미참여 시 null", example = "2")
    private Long myOptionId;

    @Schema(description = "투표 선택지 목록")
    private List<PollOptionResponse> options;
}
