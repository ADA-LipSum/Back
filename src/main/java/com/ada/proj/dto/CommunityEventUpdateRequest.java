package com.ada.proj.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "이벤트 위젯 수정 요청 (전달한 필드만 업데이트)")
public class CommunityEventUpdateRequest {

    @Size(max = 100, message = "제목은 100자 이내여야 합니다.")
    @Schema(description = "이벤트 제목", example = "2026 여름 해커톤 (수정)")
    private String title;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "시작일 (yyyy-MM-dd)", example = "2026-07-01")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "종료일 (yyyy-MM-dd)", example = "2026-07-03")
    private LocalDate endDate;

    @Size(max = 200, message = "장소는 200자 이내여야 합니다.")
    @Schema(description = "장소", example = "서울 강남구 코엑스")
    private String location;

    @Schema(description = "설명")
    private String description;

    @Schema(description = "관련 링크")
    private String relatedLink;
}
