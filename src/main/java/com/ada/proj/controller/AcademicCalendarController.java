package com.ada.proj.controller;

import java.time.LocalDate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.dto.AcademicCalendarResponse;
import com.ada.proj.dto.ApiResponse;
import com.ada.proj.service.AcademicCalendarService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/calendar")
@Tag(name = "학사 일정", description = "NEIS 연동 경북SW마이스터고 학사 일정 — 월별 행사·일정 조회. 결과는 Redis에 6시간 캐싱.")
public class AcademicCalendarController {

    private final AcademicCalendarService academicCalendarService;

    @GetMapping
    @Operation(
            summary = "학사 일정 월별 조회",
            description = """
                    특정 연월의 학사 일정을 반환합니다. **인증 불필요.**

                    - `year`·`month` 생략 시 **현재 월**로 조회됩니다.
                    - 결과는 Redis에 **6시간** 캐싱됩니다.
                    - NEIS에 해당 월 데이터가 없으면 `events`가 빈 배열로 반환됩니다.

                    **Response 구조**
                    ```json
                    {
                      "year": 2026,
                      "month": 6,
                      "events": [
                        { "date": "20260601", "eventName": "현충일", "content": null },
                        { "date": "20260603", "eventName": "중간고사", "content": "1~3학년" }
                      ]
                    }
                    ```
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "학사 일정 반환 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "연도·월 범위 오류",
                    content = @Content(examples = @ExampleObject(value = """
                            {"success":false,"errorCode":"BAD_REQUEST","message":"월은 1~12 사이여야 합니다."}""")))
    })
    public ResponseEntity<?> getCalendar(
            @Parameter(description = "조회 연도 (예: 2026)", example = "2026")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "조회 월 (1~12)", example = "6")
            @RequestParam(required = false) Integer month
    ) {
        LocalDate today = LocalDate.now();
        int targetYear  = (year  != null) ? year  : today.getYear();
        int targetMonth = (month != null) ? month : today.getMonthValue();

        if (targetMonth < 1 || targetMonth > 12) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("BAD_REQUEST", "월은 1~12 사이여야 합니다."));
        }
        if (targetYear < 2000 || targetYear > 2100) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("BAD_REQUEST", "연도가 올바르지 않습니다."));
        }

        return ResponseEntity.ok(ApiResponse.success(
                academicCalendarService.getMonthlyCalendar(targetYear, targetMonth)));
    }
}
