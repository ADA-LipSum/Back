package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "AcademicScheduleRequest", description = "학사 일정 등록·수정 요청")
public class AcademicScheduleRequest {

    @NotNull
    @Schema(description = "행사 날짜 (yyyy-MM-dd)", example = "2026-09-01")
    private java.time.LocalDate eventDate;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "행사명 (최대 100자)", example = "개학식")
    private String eventName;

    @Size(max = 500)
    @Schema(description = "행사 내용 (최대 500자, 선택)", example = "2학기 개학식 및 조례")
    private String content;
}
