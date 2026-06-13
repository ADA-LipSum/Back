package com.ada.proj.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.PointExchangeRateResponse;
import com.ada.proj.dto.PointExchangeRateUpdateRequest;
import com.ada.proj.dto.PointExchangeRequest;
import com.ada.proj.dto.PointExchangeResponse;
import com.ada.proj.exception.UnauthenticatedException;
import com.ada.proj.service.PointExchangeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "포인트 교환", description = "포인트를 코인으로 교환하는 API (포인트 -> 코인 단방향만 가능)")
public class PointExchangeController {

    private final PointExchangeService pointExchangeService;

    @GetMapping("/api/points/exchange-rate")
    @Operation(summary = "포인트->코인 교환 비율/한도 조회", description = "코인 1개로 교환하는 데 필요한 포인트 수와 월별 최대 교환 가능 코인 수를 조회합니다. **JWT 인증 필요.**")
    public ApiResponse<PointExchangeRateResponse> getRate() {
        return ApiResponse.success(pointExchangeService.getRate());
    }

    @PutMapping("/api/admin/points/exchange-rate")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(
            summary = "포인트->코인 교환 비율/한도 수정",
            description = """
                    관리자/선생님이 코인 1개로 교환하는 데 필요한 포인트 수(pointsPerCoin)와
                    1인당 월 최대 교환 가능 코인 수(monthlyCoinLimit)를 변경합니다.
                    요청 본문에 포함(non-null)된 필드만 변경됩니다.
                    """
    )
    public ApiResponse<PointExchangeRateResponse> updateRate(@Valid @RequestBody PointExchangeRateUpdateRequest req) {
        return ApiResponse.success(pointExchangeService.updateRate(req));
    }

    @PostMapping("/api/points/exchange")
    @Operation(
            summary = "포인트 -> 코인 교환",
            description = """
                    본인의 포인트를 코인으로 교환합니다. (포인트 -> 코인 단방향만 가능, 코인을 포인트로 되돌릴 수는 없습니다)
                    교환 비율의 배수가 아닌 포인트를 요청하면 비율의 배수만큼만 차감되고 나머지 포인트는 그대로 남습니다.
                    1인당 월간 교환 가능한 코인 수에는 한도(monthlyCoinLimit, 기본 100코인)가 있으며 매월 1일(KST) 초기화됩니다.
                    보유 포인트가 부족하거나 교환 가능한 최소 단위(교환 비율)보다 적게 요청하면 400,
                    이번 달 교환 한도를 초과하면 409가 반환됩니다. **JWT 인증 필요.**
                    """
    )
    public ApiResponse<PointExchangeResponse> exchange(@Valid @RequestBody PointExchangeRequest req, Authentication auth) {
        return ApiResponse.success(pointExchangeService.exchange(requireUuid(auth), req.getPoints()));
    }

    private String requireUuid(Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new UnauthenticatedException("Unauthenticated");
        }
        return auth.getName();
    }
}
