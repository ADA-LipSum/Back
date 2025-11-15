package com.ada.proj.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.StoreItemCreateRequest;
import com.ada.proj.dto.StoreItemResponse;
import com.ada.proj.service.StoreItemService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/store/items")
@RequiredArgsConstructor
@Tag(name = "상점", description = "상점 물품 등록 / 조회 API")
public class StoreItemController {

    private final StoreItemService storeItemService;

    @PostMapping
    @Operation(summary = "상점 물품 등록", description = "Teacher만 상점에 상품을 등록할 수 있습니다.")
    public ApiResponse<StoreItemResponse> createItem(
            @Valid @RequestBody StoreItemCreateRequest req,
            Authentication auth
    ) {
        var item = storeItemService.createItem(req, auth);
        return ApiResponse.success(StoreItemResponse.from(item));
    }
}