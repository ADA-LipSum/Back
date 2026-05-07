package com.ada.proj.dto;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "PollCreateRequest", description = "디스코드 방식 투표 생성 요청")
public class PollCreateRequest {

    @NotBlank
    @Size(max = 100)
    @Schema(description = "투표 질문. 최대 100자", example = "다음 프로젝트 DB는 무엇으로 할까요?")
    private String question;

    @NotEmpty
    @Size(min = 2, max = 10)
    @Schema(description = "투표 선택지 목록. 2개 이상 10개 이하", example = "[\"MySQL\", \"PostgreSQL\", \"MongoDB\"]")
    private List<@NotBlank @Size(max = 80) String> options;

    @Future
    @Schema(description = "투표 종료 시각. 현재 이후 시각이어야 합니다.", example = "2026-05-08T18:00:00")
    private LocalDateTime endsAt;

    @Schema(description = "익명 투표 여부. true이면 투표자 목록을 응답하지 않습니다.", example = "false")
    private Boolean anonymous;
}
