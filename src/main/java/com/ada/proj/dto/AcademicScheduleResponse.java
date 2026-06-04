package com.ada.proj.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AcademicScheduleResponse", description = "학사 일정 항목")
public class AcademicScheduleResponse {

    @Schema(description = "일정 ID", example = "1")
    private Long id;

    @Schema(description = "행사 날짜", example = "2026-09-01")
    private LocalDate eventDate;

    @Schema(description = "행사명", example = "개학식")
    private String eventName;

    @Schema(description = "행사 내용", example = "2학기 개학식 및 조례")
    private String content;

    @Schema(description = "등록 시각")
    private LocalDateTime createdAt;
}
