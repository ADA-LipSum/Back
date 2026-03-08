package com.ada.proj.controller;

import com.ada.proj.dto.*;
import com.ada.proj.entity.UserCoins;
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
            @Parameter(description = "대상 사용자 UUID", example = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
            @PathVariable String userUuid,
            Authentication auth) {
        ensureCoinViewPermission(auth, userUuid);
        int balance = coinsService.getBalance(userUuid);
        return ApiResponse.success(new CoinsBalanceResponse(userUuid, balance));
    }

    @GetMapping("/balance")
    @Operation(summary = "코인 잔액 조회(쿼리)", description = "본인 또는 ADMIN이 특정 사용자의 현재 코인 잔액을 조회합니다. 예: /api/coins/balance?userUuid=...")
    public ApiResponse<CoinsBalanceResponse> getBalanceQuery(
            @Parameter(description = "대상 사용자 UUID", example = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
            @RequestParam String userUuid,
            Authentication auth) {
        return getBalance(userUuid, auth);
    }

    @PostMapping("/adjustments")
    @Operation(summary = "코인 조정", description = "type에 따라 코인을 지급(GAIN), 차감(LOSS), 사용(USE) 합니다.")
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
            @Parameter(description = "대상 사용자 UUID", example = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
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
