package com.ada.proj.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.PurchaseItemRequest;
import com.ada.proj.dto.PurchaseItemResponse;
import com.ada.proj.service.PurchaseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/store/purchase")
@RequiredArgsConstructor
@Tag(name = "상점", description = "상점 아이템 구매 API")
public class PurchaseController {

    private final PurchaseService purchaseService;

    @PostMapping
    @Operation(summary = "아이템 구매", description = "포인트로 상점 아이템을 구매합니다.")
    public ApiResponse<PurchaseItemResponse> purchase(
            @Valid @RequestBody PurchaseItemRequest req,
            Authentication auth
    ) {
        var history = purchaseService.purchase(req, auth);
        return ApiResponse.success(PurchaseItemResponse.from(history));
    }
}