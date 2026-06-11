package com.ada.proj.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.AttendanceResponse;
import com.ada.proj.exception.UnauthenticatedException;
import com.ada.proj.service.AttendanceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Tag(name = "출석체크", description = "일일 출석체크 및 포인트/코인 지급 API")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/check-in")
    @Operation(
            summary = "출석체크",
            description = """
                    오늘 처음 호출 시 출석으로 기록되고 포인트/코인이 지급됩니다.
                    이미 오늘 출석체크를 완료한 경우 409 Conflict가 반환됩니다. **JWT 인증 필요.**
                    """
    )
    public ApiResponse<AttendanceResponse> checkIn(Authentication auth) {
        return ApiResponse.success(attendanceService.checkIn(requireUuid(auth)));
    }

    @GetMapping("/status")
    @Operation(summary = "출석체크 상태 조회", description = "오늘 출석 여부와 누적 출석 일수를 조회합니다. **JWT 인증 필요.**")
    public ApiResponse<AttendanceResponse> status(Authentication auth) {
        return ApiResponse.success(attendanceService.getStatus(requireUuid(auth)));
    }

    private String requireUuid(Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new UnauthenticatedException("Unauthenticated");
        }
        return auth.getName();
    }
}
