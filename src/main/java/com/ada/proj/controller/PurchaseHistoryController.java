package com.ada.proj.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.PurchaseHistoryResponse;
import com.ada.proj.service.PurchaseHistoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/store/purchase/logs")
@RequiredArgsConstructor
@Tag(name = "상점", description = "구매 기록 조회(통합) API")
public class PurchaseHistoryController {

    private final PurchaseHistoryService purchaseHistoryService;

    @GetMapping
    @Operation(
            summary = "구매 기록 조회(통합)",
            description = "기간별, 아이템별, 구매자별로 조회 가능. Teacher/Admin 전체 조회 가능, 학생은 본인만 조회."
    )
    public ApiResponse<List<PurchaseHistoryResponse>> getLogs(
            @RequestParam(required = false) String userUuid,
            @RequestParam(required = false) String itemUuid,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Authentication auth
    ) {
        var data = purchaseHistoryService.filter(userUuid, itemUuid, startDate, endDate, auth);
        return ApiResponse.success(data);
    }
}