package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AcademicScheduleEvent", description = "학사 일정 이벤트 항목")
public class AcademicScheduleEvent {

    @Schema(description = "날짜 (yyyyMMdd)", example = "20260603")
    private String date;

    @Schema(description = "행사명", example = "중간고사")
    private String eventName;

    @Schema(description = "행사 내용 (없을 수 있음)", example = "1~3학년 중간고사 시작")
    private String content;
}
