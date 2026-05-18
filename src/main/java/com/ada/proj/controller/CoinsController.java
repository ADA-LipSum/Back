package com.ada.proj.controller;

import com.ada.proj.dto.*;
import com.ada.proj.entity.UserCoins;
import com.ada.proj.enums.PointChangeType;
import com.ada.proj.service.CoinsService;
import com.ada.proj.service.UserService;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/coins")
@RequiredArgsConstructor
@Validated
@Tag(name = "코인", description = "코인 지급/차감/사용 및 잔액 조회 API")
public class CoinsController {

    private final CoinsService coinsService;
    private final UserService userService;

    @GetMapping("/balance/{userUuid}")
    @Operation(summary = "코인 잔액 조회", description = "본인 또는 ADMIN이 특정 사용자의 현재 코인 잔액을 조회합니다.")
    public ApiResponse<CoinsBalanceResponse> getBalance(
            @Parameter(description = "대상 사용자 UUID")
            @PathVariable String userUuid,
            Authentication auth) {
        ensureCoinViewPermission(auth, userUuid);
        int balance = coinsService.getBalance(userUuid);
        return ApiResponse.success(new CoinsBalanceResponse(userUuid, balance));
    }

    @GetMapping("/balance")
    @Operation(summary = "코인 잔액 조회(쿼리)", description = "본인 또는 ADMIN이 특정 사용자의 현재 코인 잔액을 조회합니다. 예: /api/coins/balance?userUuid=...")
    public ApiResponse<CoinsBalanceResponse> getBalanceQuery(
            @Parameter(description = "대상 사용자 UUID")
            @RequestParam String userUuid,
            Authentication auth) {
        return getBalance(userUuid, auth);
    }

    @PostMapping("/adjustments")
    @Operation(
            summary = "코인 조정",
            description = """
                    코인을 지급/차감/사용 처리합니다. ADMIN만 가능합니다.

                    **Request Body:**
                    - `userUuid` (필수): 대상 사용자 UUID
                    - `type` (필수): 조정 유형 (GAIN: 지급 | LOSS: 차감 | USE: 사용)
                    - `coins` (필수): 코인 수량 (최소 1)
                    - `description` (선택): 조정 사유 또는 메모

                    **Response:**
                    - `userUuid`: 대상 사용자 UUID
                    - `type`: 조정 유형
                    - `coins`: 조정된 코인 수량
                    - `balance`: 조정 후 잔액
                    - `description`: 메모
                    - `createdAt`: 조정 시각

                    ADMIN이 아닌 경우 403을 반환합니다.
                    """
    )
    public ApiResponse<CoinsTransactionResponse> adjust(
            @Valid @RequestBody CoinsAdjustRequest req,
            Authentication auth) {

        // admin only
        ensureAdmin(auth);

        UserCoins tx = switch (req.getType()) {
            case GAIN ->
                coinsService.grantCoins(req.getUserUuid(), req.getCoins(), req.getDescription());
            case LOSS ->
                coinsService.deductCoins(req.getUserUuid(), req.getCoins(), req.getDescription());
            case USE ->
                coinsService.useCoins(req.getUserUuid(), req.getCoins(), req.getDescription());
            default ->
                throw new IllegalArgumentException("지원하지 않는 type입니다: " + req.getType());
        };

        return ApiResponse.success(CoinsTransactionResponse.from(tx));
    }

    @GetMapping("/transactions")
    @Operation(summary = "코인 거래내역 조회", description = "특정 사용자(userUuid)의 코인 거래내역을 최신순으로 페이징하여 조회합니다.")
    public ApiResponse<PageResponse<CoinsTransactionResponse>> getTransactions(
            @Parameter(description = "대상 사용자 UUID")
            @RequestParam String userUuid,
            @Parameter(description = "페이지(0부터)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {

        ensureSelfOrAdmin(auth, userUuid);

        var pageResult = coinsService.getTransactions(userUuid, page, size).map(CoinsTransactionResponse::from);
        PageResponse<CoinsTransactionResponse> body = new PageResponse<>(
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.getContent());

        return ApiResponse.success(body);
    }

    private void ensureSelfOrAdmin(Authentication auth, String userUuid) {
        if (auth == null) {
            throw new SecurityException("Unauthenticated");
        }
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !auth.getName().equals(userUuid)) {
            throw new SecurityException("Forbidden");
        }
    }

    private void ensureAdmin(Authentication auth) {
        if (auth == null) {
            throw new SecurityException("Unauthenticated");
        }
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new SecurityException("Forbidden");
        }
    }

    private void ensureAdminOrTeacher(Authentication auth) {
        if (auth == null) {
            throw new SecurityException("Unauthenticated");
        }
        boolean ok = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_TEACHER"));
        if (!ok) {
            throw new SecurityException("Forbidden");
        }
    }

    @PostMapping("/adjustments/bulk")
    @Operation(
            summary = "역할별 코인 일괄 지급/차감 (관리자/선생님)",
            description = """
                    특정 역할(STUDENT 등)의 모든 사용자에게 코인을 일괄 지급하거나 차감합니다.
                    ADMIN/TEACHER 전용입니다.

                    **Request Body:**
                    - `role` (필수): 대상 역할 (STUDENT | TEACHER | ADMIN)
                    - `type` (필수): 조정 유형 (GAIN: 지급 | LOSS: 차감)
                    - `coins` (필수): 코인 수량 (최소 1)
                    - `description` (선택): 지급 사유

                    **Response:**
                    - `totalTargets`: 대상 인원 수
                    - `successCount`: 처리 성공 건수
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth"),
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "일괄 처리 완료"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 type (GAIN/LOSS만 허용)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN/TEACHER만 가능)")
            }
    )
    public ApiResponse<BulkGrantResultResponse> bulkAdjust(
            @Valid @RequestBody BulkCoinAdjustRequest req,
            Authentication auth) {
        ensureAdminOrTeacher(auth);
        if (req.getType() != PointChangeType.GAIN && req.getType() != PointChangeType.LOSS) {
            throw new IllegalArgumentException("일괄 지급은 GAIN 또는 LOSS만 지원합니다.");
        }
        BulkGrantResultResponse result = coinsService.bulkGrantByRole(req.getRole(), req.getType(), req.getCoins(), req.getDescription());
        return ApiResponse.success(result);
    }

    @GetMapping("/adjustments/history")
    @Operation(
            summary = "코인 지급 내역 조회 (관리자/선생님)",
            description = """
                    관리자/선생님이 지급·차감(GAIN/LOSS)한 전체 코인 조정 내역을 최신순으로 조회합니다.
                    ADMIN/TEACHER 전용입니다.

                    **Query Parameters:**
                    - `page` (선택): 페이지 번호 (기본값: 0)
                    - `size` (선택): 페이지 크기 (기본값: 20)

                    **Response:** 페이징된 코인 트랜잭션 목록 (coinUuid, userUuid, changeType, coins, balanceAfter, description, createdAt)
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth"),
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN/TEACHER만 가능)")
            }
    )
    public ApiResponse<PageResponse<CoinsTransactionResponse>> getAdjustmentHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        ensureAdminOrTeacher(auth);
        var pageResult = coinsService.getAdjustmentHistory(page, size).map(CoinsTransactionResponse::from);
        return ApiResponse.success(new PageResponse<>(
                pageResult.getNumber(), pageResult.getSize(),
                pageResult.getTotalElements(), pageResult.getTotalPages(),
                pageResult.getContent()));
    }

    private void ensureCoinViewPermission(Authentication auth, String targetUuid) {
        if (auth == null) {
            throw new SecurityException("Unauthenticated");
        }

        String currentUuid = auth.getName();

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        boolean isTeacher = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));

        boolean isStudent = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        if (isAdmin) {
            return;
        }

        if (isStudent) {
            if (currentUuid.equals(targetUuid)) {
                return;
            }
            throw new SecurityException("Forbidden: 학생 계정은 자신의 코인만 조회할 수 있습니다.");
        }

        if (isTeacher) {
            boolean targetIsStudent = userService.isStudent(targetUuid);
            if (targetIsStudent) {
                return;
            }
            throw new SecurityException("Forbidden: 선생님 계정은 학생 코인만 조회할 수 있습니다.");
        }

        throw new SecurityException("Forbidden");
    }
}
