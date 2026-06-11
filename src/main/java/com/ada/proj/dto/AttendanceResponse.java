package com.ada.proj.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "출석체크 상태")
public class AttendanceResponse {

    @Schema(description = "조회 기준 날짜 (Asia/Seoul)")
    private LocalDate attendanceDate;

    @Schema(description = "오늘 출석체크 완료 여부")
    private boolean checkedInToday;

    @Schema(description = "누적 출석 일수")
    private long totalCount;
}
