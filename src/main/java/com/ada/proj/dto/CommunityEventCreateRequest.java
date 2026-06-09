package com.ada.proj.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "이벤트 위젯 생성 요청")
public class CommunityEventCreateRequest {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이내여야 합니다.")
    @Schema(description = "이벤트 제목", example = "2026 여름 해커톤")
    private String title;

    @NotNull(message = "시작일은 필수입니다.")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "시작일 (yyyy-MM-dd)", example = "2026-07-01")
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다.")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "종료일 (yyyy-MM-dd)", example = "2026-07-03")
    private LocalDate endDate;

    @NotBlank(message = "장소는 필수입니다.")
    @Size(max = 200, message = "장소는 200자 이내여야 합니다.")
    @Schema(description = "장소", example = "서울 강남구 코엑스")
    private String location;

    @Schema(description = "설명 (선택)", example = "팀별로 48시간 동안 프로젝트를 완성합니다.")
    private String description;

    @Schema(description = "관련 링크 (공지사항 URL 등)", example = "https://example.com/notices/1")
    private String relatedLink;
}
