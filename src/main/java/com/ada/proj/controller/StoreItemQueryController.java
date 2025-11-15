package com.ada.proj.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.StoreItemResponse;
import com.ada.proj.entity.StoreType;
import com.ada.proj.service.StoreItemQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/store/items")
@RequiredArgsConstructor
@Tag(name = "상점", description = "상점 아이템 조회/검색 API")
public class StoreItemQueryController {

    private final StoreItemQueryService storeItemQueryService;

    @GetMapping("/{itemUuid}")
    @Operation(summary = "상점 아이템 상세 조회")
    public ApiResponse<StoreItemResponse> detail(@PathVariable String itemUuid) {
        return ApiResponse.success(storeItemQueryService.getItemDetail(itemUuid));
    }

    @GetMapping("/search")
    @Operation(
            summary = "상점 아이템 검색/필터링",
            description = """
                    필터 조건:
                    - name: 이름 부분 검색
                    - minPrice, maxPrice: 가격 범위
                    - category: 카테고리
                    - storeType: ITEM(물품), DECORATION(장식)
                    - sort: name(기본), price_asc, price_desc
                    """
    )
    public ApiResponse<List<StoreItemResponse>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) StoreType storeType,
            @RequestParam(required = false) String sort
    ) {
        var result = storeItemQueryService.searchItems(name, minPrice, maxPrice, category, storeType, sort);
        return ApiResponse.success(result);
    }
}