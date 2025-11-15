package com.ada.proj.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.TradeItemCreateRequest;
import com.ada.proj.dto.TradeItemResponse;
import com.ada.proj.dto.TradeLogCreateRequest;
import com.ada.proj.dto.TradeLogResponse;
import com.ada.proj.dto.TradePurchaseRequest;
import com.ada.proj.dto.TradePurchaseResponse;
import com.ada.proj.dto.PageResponse;
import com.ada.proj.entity.TradeItem;
import com.ada.proj.entity.TradeLog;
import com.ada.proj.entity.TradeCategory;
import com.ada.proj.service.TradeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trade")
@RequiredArgsConstructor
@Validated
@Tag(name = "거래소", description = "거래 목록/구매/로그 API")
public class TradeController {

    private final TradeService tradeService;

    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(
        summary = "거래 목록 추가",
        description = "ADMIN/TEACHER만 거래 아이템을 등록할 수 있습니다.\n\n"
                + "요청 필드 설명:\n"
                + "- name: 아이템 이름(상품명).\n"
                + "- description: 아이템 설명(옵션).\n"
                + "- price: 포인트 기준 가격(최소 1).\n"
                + "- stock: 초기 재고 수량(0 이상).\n"
                + "- active: 판매 활성화 여부(기본 true).\n"
                + "- category: 카테고리(FOOD | TOOLS | ETC).\n"
                + "- imageUrl: 대표 이미지 URL(옵션)."
    )
    public ApiResponse<TradeItemResponse> createItem(@Valid @RequestBody TradeItemCreateRequest req, Authentication auth) {
        String creatorUuid = auth != null ? auth.getName() : null;
        TradeItem item = tradeService.createItem(req, creatorUuid);
        return ApiResponse.success(TradeItemResponse.from(item));
    }

    @GetMapping("/items/{itemUuid}")
    @Operation(summary = "아이템 상세 조회", description = "itemUuid로 아이템 상세 정보를 조회합니다.")
    public ApiResponse<TradeItemResponse> getItem(
            @Parameter(description = "아이템 UUID", example = "1b6c2a5f-0d7a-4d09-9c4a-1f2e3a4b5c6d")
            @PathVariable String itemUuid) {
        TradeItem item = tradeService.getItemDetail(itemUuid);
        return ApiResponse.success(TradeItemResponse.from(item));
    }

    @GetMapping("/items/search")
    @Operation(summary = "아이템 검색/필터", description = "검색어/카테고리/가격범위/정렬/페이지네이션으로 아이템을 조회합니다.")
        public ApiResponse<PageResponse<TradeItemResponse>> searchItems(
            @Parameter(description = "검색어(이름/설명 LIKE)", example = "배지")
            @RequestParam(name = "q", required = false) String keyword,
            @Parameter(description = "카테고리 필터(FOOD|TOOLS|ETC)", example = "TOOLS")
            @RequestParam(name = "category", required = false) TradeCategory category,
            @Parameter(description = "최소 가격(포인트)", example = "50")
            @RequestParam(name = "minPrice", required = false) Integer minPrice,
            @Parameter(description = "최대 가격(포인트)", example = "200")
            @RequestParam(name = "maxPrice", required = false) Integer maxPrice,
            @Parameter(description = "판매 활성 필터", example = "true")
            @RequestParam(name = "active", required = false, defaultValue = "true") Boolean active,
            @Parameter(description = "페이지(0부터)", example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size,
            @Parameter(description = "정렬 필드(createdAt|price|name)", example = "price")
            @RequestParam(name = "sort", required = false, defaultValue = "createdAt") String sort,
            @Parameter(description = "정렬 방향(asc|desc)", example = "asc")
            @RequestParam(name = "dir", required = false, defaultValue = "desc") String dir
        ) {
        var pageResult = tradeService.searchItems(keyword, category, minPrice, maxPrice, active, page, size, sort, dir)
                .map(TradeItemResponse::from);
        PageResponse<TradeItemResponse> body = new PageResponse<>(
                pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements(), pageResult.getTotalPages(), pageResult.getContent());
        return ApiResponse.success(body);
    }

    @PostMapping("/purchase")
    @Operation(
        summary = "물품 거래(구매)",
        description = "로그인 사용자가 포인트로 물품을 구매합니다. 포인트 부족 시 실패합니다.\n\n"
                + "요청 필드 설명:\n"
                + "- itemUuid: 구매할 아이템 UUID.\n"
                + "- quantity: 구매 수량(최소 1).\n"
                + "- metadata: 추가 메타데이터(JSON 문자열)."
    )
    public ApiResponse<TradePurchaseResponse> purchase(@Valid @RequestBody TradePurchaseRequest req, Authentication auth) {
        if (auth == null) throw new SecurityException("Unauthenticated");
        String userUuid = auth.getName();
        var result = tradeService.purchase(userUuid, req);
        return ApiResponse.success(TradePurchaseResponse.of(result.getItem(), result.getLog(), result.getPointsTx()));
    }

    @PostMapping("/logs")
    @Operation(
        summary = "거래 로그 저장",
        description = "거래 로그를 별도로 저장합니다(수동 기록). 포인트 트랜잭션과 연결할 수 있습니다.\n\n"
                + "요청 필드 설명:\n"
                + "- itemUuid: 아이템 UUID.\n"
                + "- quantity: 수량(최소 1).\n"
                + "- totalPoints: 총 포인트(옵션, 미전달 시 단가*수량).\n"
                + "- itemName: 아이템 이름(옵션, 기본 DB값 사용).\n"
                + "- pointsUuid: 연결할 포인트 트랜잭션 UUID(옵션).\n"
                + "- metadata: 추가 메타데이터(JSON 문자열)."
    )
    public ApiResponse<String> createLog(@Valid @RequestBody TradeLogCreateRequest req, Authentication auth) {
        if (auth == null) throw new SecurityException("Unauthenticated");
        String userUuid = auth.getName();
        TradeLog log = tradeService.createLog(userUuid, req);
        return ApiResponse.success(log.getLogUuid());
    }

    @GetMapping("/my/logs")
    @Operation(summary = "내 구매내역 조회", description = "로그인 사용자의 구매내역을 최신순으로 페이징 조회합니다.")
        public ApiResponse<PageResponse<TradeLogResponse>> myLogs(
            @Parameter(description = "페이지(0부터)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        if (auth == null) throw new SecurityException("Unauthenticated");
        String userUuid = auth.getName();
        var pageResult = tradeService.getMyLogs(userUuid, page, size).map(TradeLogResponse::from);
        PageResponse<TradeLogResponse> body = new PageResponse<>(
                pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements(), pageResult.getTotalPages(), pageResult.getContent());
        return ApiResponse.success(body);
    }

    @GetMapping("/users/{userUuid}/logs")
    @Operation(
        summary = "사용자 구매내역 조회",
        description = "특정 사용자(UUID)의 구매내역을 최신순으로 페이징 조회합니다. 본인 또는 ADMIN/TEACHER만 열람 가능."
    )
    public ApiResponse<PageResponse<TradeLogResponse>> userLogs(
            @Parameter(description = "대상 사용자 UUID", example = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
            @PathVariable String userUuid,
            @Parameter(description = "페이지(0부터)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        ensureSelfOrAdminOrTeacher(auth, userUuid);
        var pageResult = tradeService.getMyLogs(userUuid, page, size).map(TradeLogResponse::from);
        PageResponse<TradeLogResponse> body = new PageResponse<>(
                pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements(), pageResult.getTotalPages(), pageResult.getContent());
        return ApiResponse.success(body);
    }

    private void ensureSelfOrAdminOrTeacher(Authentication auth, String userUuid) {
        if (auth == null) throw new SecurityException("Unauthenticated");
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isTeacher = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));
        if (!isAdmin && !isTeacher && !auth.getName().equals(userUuid)) {
            throw new SecurityException("Forbidden");
        }
    }
}
