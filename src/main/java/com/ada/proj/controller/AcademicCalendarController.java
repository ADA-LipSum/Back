package com.ada.proj.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.dto.AcademicCalendarResponse;
import com.ada.proj.dto.AcademicScheduleRequest;
import com.ada.proj.dto.AcademicScheduleResponse;
import com.ada.proj.dto.ApiResponse;
import com.ada.proj.service.AcademicCalendarService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/calendar")
@Tag(name = "학사 일정", description = "학사 일정 월별 조회 + 관리자·교사 직접 등록·수정·삭제")
public class AcademicCalendarController {

    private final AcademicCalendarService academicCalendarService;

    // ── 공개 조회 ──────────────────────────────────────────────────────────

    @GetMapping
    @Operation(
            summary = "학사 일정 월별 조회",
            description = """
                    특정 연월의 학사 일정을 반환합니다. **인증 불필요.**

                    - `year`·`month` 생략 시 **현재 월**로 조회됩니다.
                    - 관리자·교사가 직접 등록한 일정이 반환됩니다.

                    **Response:** `events` 배열 (날짜 오름차순)
                    """
    )
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

    @GetMapping("/year")
    @Operation(
            summary = "학사 일정 연간 전체 조회",
            description = "특정 연도의 학사 일정 전체를 반환합니다. 날짜 오름차순. **인증 불필요.**"
    )
    public ResponseEntity<ApiResponse<List<AcademicScheduleResponse>>> getYearlyCalendar(
            @Parameter(description = "조회 연도", example = "2026")
            @RequestParam(required = false) Integer year
    ) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(ApiResponse.success(
                academicCalendarService.listAll(targetYear)));
    }

    // ── 관리자·교사 CRUD ───────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(
            summary = "학사 일정 등록 (관리자·교사)",
            description = """
                    학사 일정을 새로 등록합니다. **관리자 또는 교사만 가능.**

                    | 필드 | 필수 | 설명 |
                    |---|---|---|
                    | eventDate | ✅ | 날짜 (`yyyy-MM-dd`) |
                    | eventName | ✅ | 행사명 (최대 100자) |
                    | content | - | 행사 내용 (최대 500자) |

                    **Response:** `data` = 생성된 일정 정보
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<AcademicScheduleResponse>> create(
            @Valid @RequestBody AcademicScheduleRequest request,
            @Parameter(hidden = true) Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        academicCalendarService.create(request, authentication)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(
            summary = "학사 일정 수정 (관리자·교사)",
            description = "학사 일정을 수정합니다. **관리자 또는 교사만 가능.**",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<AcademicScheduleResponse>> update(
            @Parameter(description = "일정 ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody AcademicScheduleRequest request,
            @Parameter(hidden = true) Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                academicCalendarService.update(id, request, authentication)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(
            summary = "학사 일정 삭제 (관리자·교사)",
            description = "학사 일정을 삭제합니다. **관리자 또는 교사만 가능.**",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "일정 ID", example = "1") @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication
    ) {
        academicCalendarService.delete(id, authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
