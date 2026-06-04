package com.ada.proj.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AcademicCalendarResponse", description = "학사 일정 월별 응답")
public class AcademicCalendarResponse {

    @Schema(description = "조회 연도", example = "2026")
    private int year;

    @Schema(description = "조회 월 (1~12)", example = "6")
    private int month;

    @Schema(description = "해당 월의 학사 일정 목록 (날짜 오름차순)")
    private List<AcademicScheduleEvent> events;
}
