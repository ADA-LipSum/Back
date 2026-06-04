package com.ada.proj.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.service.MealService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meal")
@Tag(name = "급식", description = "NEIS 연동 급식 정보 — 조식·중식·석식, Redis 12시간 캐싱")
public class MealController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final MealService mealService;

    @GetMapping
    @Operation(
            summary = "급식 조회",
            description = """
                    특정 날짜의 급식 정보를 반환합니다. **인증 불필요.**

                    - `date` 생략 시 **오늘** 날짜로 조회됩니다.
                    - 결과는 Redis에 **12시간** 캐싱됩니다.
                    - 조·중·석식 중 하나라도 데이터가 있어야 캐시에 저장됩니다.
                    - NEIS에 해당 날짜 데이터가 없으면 해당 끼니가 `null` 로 반환됩니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "날짜 형식 오류",
                    content = @Content(examples = @ExampleObject(value = """
                            {"success":false,"errorCode":"BAD_REQUEST","message":"날짜 형식이 올바르지 않습니다. yyyyMMdd 형식으로 입력해주세요."}""")))
    })
    public ResponseEntity<?> getMeal(
            @Parameter(description = "조회 날짜 (yyyyMMdd). 생략 시 오늘", example = "20260604")
            @RequestParam(required = false) String date
    ) {
        LocalDate targetDate = parseDate(date);
        if (targetDate == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("날짜 형식이 올바르지 않습니다. yyyyMMdd 형식으로 입력해주세요."));
        }
        return ResponseEntity.ok(ApiResponse.success(mealService.getMeal(targetDate)));
    }

    @DeleteMapping("/cache")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "급식 캐시 초기화 (관리자)",
            description = """
                    Redis에 저장된 급식 캐시를 삭제합니다. **관리자 전용.**

                    - `date` 지정 시 해당 날짜 하루만 삭제
                    - `from`·`to` 지정 시 날짜 범위 삭제
                    - 아무 파라미터 없으면 **오늘** 캐시만 삭제

                    캐시 삭제 후 다음 조회 시 NEIS API를 다시 호출합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> clearCache(
            @Parameter(description = "삭제할 날짜 (yyyyMMdd). 단일 날짜 삭제", example = "20260604")
            @RequestParam(required = false) String date,
            @Parameter(description = "범위 삭제 시작일 (yyyyMMdd)", example = "20260601")
            @RequestParam(required = false) String from,
            @Parameter(description = "범위 삭제 종료일 (yyyyMMdd)", example = "20260630")
            @RequestParam(required = false) String to
    ) {
        if (from != null && to != null) {
            LocalDate fromDate = parseDate(from);
            LocalDate toDate   = parseDate(to);
            if (fromDate == null || toDate == null) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("날짜 형식이 올바르지 않습니다."));
            }
            mealService.clearRangeCache(fromDate, toDate);
        } else {
            LocalDate target = (date != null) ? parseDate(date) : LocalDate.now();
            if (target == null) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("날짜 형식이 올바르지 않습니다."));
            }
            mealService.clearCache(target);
        }
        return ResponseEntity.ok(ApiResponse.success());
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) return LocalDate.now();
        try {
            return LocalDate.parse(date, DATE_FMT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
