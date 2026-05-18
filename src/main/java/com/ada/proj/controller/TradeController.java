//C:\Users\russe\Documents\GitHub\Ada\Back\src\main\java\com\ada\proj\controller\TradeController.java
package com.ada.proj.controller;

import com.ada.proj.dto.*;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.ada.proj.enums.TradeCategory;
import com.ada.proj.entity.TradeItem;
import com.ada.proj.entity.TradeLog;
import com.ada.proj.service.CartService;
import com.ada.proj.service.TradeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/trade")
@RequiredArgsConstructor
@Validated
@Tag(name = "거래소", description = "거래 목록/구매/로그 API")
public class TradeController {

    private final TradeService tradeService;
    private final CartService cartService;

    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(
            summary = "거래 목록 추가",
            description = "ADMIN/TEACHER만 거래 아이템을 등록할 수 있습니다.\n\n"
            + "요청 필드 설명:\n"
            + "- name: 아이템 이름(상품명).\n"
            + "- description: 아이템 설명(옵션).\n"
            + "- price: 가격(FOOD=코인, ETC=포인트 / 최소 1).\n"
            + "- active: 판매 활성화 여부(기본 true).\n"
            + "- category: 카테고리(FOOD | ETC).\n"
            + "- subCategory: 서브카테고리(SNACK|CANDY|JUICE|INSTANT|STICKER|BANNER).\n"
            + "- imageUrl: 대표 이미지 URL(옵션)."
    )
    public ApiResponse<TradeItemResponse> createItem(@Valid @RequestBody TradeItemCreateRequest req, Authentication auth) {
        String creatorUuid = auth != null ? auth.getName() : null;
        TradeItem item = tradeService.createItem(req, creatorUuid);
        return ApiResponse.success(TradeItemResponse.from(item));
    }

    @GetMapping("/items/{itemId}")
    @Operation(
            summary = "아이템 상세 조회",
            description = "PathVariable로 itemId(UUID)를 받아 아이템 상세 정보를 조회합니다."
    )
    public ApiResponse<TradeItemResponse> getItem(
            @Parameter(description = "조회할 아이템 UUID") @PathVariable("itemId") String itemId
    ) {
        TradeItem item = tradeService.getItemDetail(itemId);
        return ApiResponse.success(TradeItemResponse.from(item));
    }

    @GetMapping("/items/search")
    @Operation(
            summary = "아이템 검색/필터 조회",
            description = "QueryString으로 검색 조건을 받아 아이템을 검색합니다.\n\n"
            + "파라미터 설명:\n"
            + "- keyword: 검색어\n"
            + "- category: FOOD|ETC\n"
            + "- subCategory: SNACK|CANDY|JUICE|INSTANT|STICKER|BANNER\n"
            + "- minPrice: 최소 가격\n"
            + "- maxPrice: 최대 가격\n"
            + "- active: 활성 여부(true|false)\n"
            + "- page: 페이지 번호\n"
            + "- size: 페이지 크기\n"
            + "- sort: 정렬 기준(createdAt|price|name)\n"
            + "- dir: 오름/내림차순(asc|desc)"
    )
    public ApiResponse<PageResponse<TradeItemResponse>> searchItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false)
            @Parameter(description = "대분류 카테고리", schema = @Schema(type = "string", allowableValues = {"FOOD", "ETC"}))
            TradeCategory category,

            @RequestParam(required = false)
            @Parameter(description = "소분류 카테고리", schema = @Schema(type = "string", allowableValues = {"SNACK", "CANDY", "JUICE", "INSTANT", "STICKER", "BANNER"}))
            TradeCategory subCategory,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false, defaultValue = "true") Boolean active,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sort,
            @RequestParam(required = false, defaultValue = "desc") String dir
    ) {
        var pageResult = tradeService
                .searchItems(keyword, category, subCategory, minPrice, maxPrice, active, page, size, sort, dir)
                .map(TradeItemResponse::from);

        return ApiResponse.success(
                new PageResponse<>(
                        pageResult.getNumber(),
                        pageResult.getSize(),
                        pageResult.getTotalElements(),
                        pageResult.getTotalPages(),
                        pageResult.getContent()
                )
        );
    }

    @PostMapping("/transactions")
    @Operation(
            summary = "물품 거래(구매)",
            description = "로그인 사용자가 물품을 구매합니다. 카테고리에 따라 결제 수단이 달라집니다.\n\n"
            + "- FOOD: 코인으로 구매\n"
            + "- ETC: 포인트로 구매\n\n"
            + "잔액 부족 시 실패합니다.\n\n"
            + "요청 필드 설명:\n"
            + "- itemUuid: 구매할 아이템 UUID.\n"
            + "- quantity: 구매 수량(최소 1)."
    )
    public ApiResponse<TradePurchaseResponse> purchase(
            @Valid @RequestBody TradePurchaseRequest req,
            Authentication auth
    ) {
        if (auth == null) {
            throw new SecurityException("Unauthenticated");
        }
        String userUuid = auth.getName();
        var result = tradeService.purchase(userUuid, req);
        return ApiResponse.success(
                TradePurchaseResponse.of(result.getItem(), result.getLog(), result.getCurrency(), result.getPointsTx(), result.getCoinsTx())
        );
    }

    @GetMapping("/logstrancsactions/me")
    @Operation(
            summary = "내 구매내역 조회",
            description = "QueryString으로 page/size를 받아 자신의 구매내역을 조회합니다.\n\n"
            + "파라미터 설명:\n"
            + "- page: 페이지 번호(default 0)\n"
            + "- size: 페이지 크기(default 20)"
    )
    public ApiResponse<PageResponse<TradeLogResponse>> myLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth
    ) {
        if (auth == null) {
            throw new SecurityException("Unauthenticated");
        }
        String userUuid = auth.getName();

        var pageResult = tradeService.getMyLogs(userUuid, page, size)
                .map(TradeLogResponse::from);

        return ApiResponse.success(
                new PageResponse<>(
                        pageResult.getNumber(),
                        pageResult.getSize(),
                        pageResult.getTotalElements(),
                        pageResult.getTotalPages(),
                        pageResult.getContent()
                )
        );
    }

    @GetMapping("/transactions")
    @Operation(
            summary = "사용자 구매내역 조회",
            description = "QueryString으로 userUuid/page/size를 받아 특정 사용자의 구매 내역을 조회합니다.\n"
            + "본인 혹은 ADMIN/TEACHER 권한만 접근 가능합니다.\n\n"
            + "파라미터 설명:\n"
            + "- userUuid: 조회 대상 사용자 UUID\n"
            + "- page: 페이지 번호(default 0)\n"
            + "- size: 페이지 크기(default 20)"
    )
    public ApiResponse<PageResponse<TradeLogResponse>> userLogs(
            @RequestParam String userUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth
    ) {
        ensureSelfOrAdminOrTeacher(auth, userUuid);

        var pageResult = tradeService.getMyLogs(userUuid, page, size)
                .map(TradeLogResponse::from);

        return ApiResponse.success(
                new PageResponse<>(
                        pageResult.getNumber(),
                        pageResult.getSize(),
                        pageResult.getTotalElements(),
                        pageResult.getTotalPages(),
                        pageResult.getContent()
                )
        );
    }

    private void ensureSelfOrAdminOrTeacher(Authentication auth, String userUuid) {
        if (auth == null) {
            throw new SecurityException("Unauthenticated");
        }
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isTeacher = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));
        if (!isAdmin && !isTeacher && !auth.getName().equals(userUuid)) {
            throw new SecurityException("Forbidden");
        }
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(
            summary = "거래 아이템 삭제",
            description = """
                선택한 거래 아이템을 삭제(비활성화)합니다.

                - 실제 DB 행을 제거하지 않고 active 플래그만 false 로 내려,
                  과거 거래 내역과 포인트 이력을 안전하게 보존합니다.
                """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<Void> deleteItem(
            @Parameter(description = "삭제할 아이템 UUID", example = "item-uuid-12345678-90ab-cdef-1234-567890abcdef")
            @PathVariable("itemId") String itemId
    ) {
        tradeService.deleteItem(itemId);
        return ApiResponse.success();
    }

    // ── 장바구니 ──────────────────────────────────────────────────────────────

    @PostMapping("/cart")
    @Operation(
            summary = "카트에 아이템 추가",
            description = """
                    카트에 아이템을 추가합니다. FOOD 카테고리 아이템만 카트에 담을 수 있습니다.

                    **Request Body:**
                    - `itemUuid` (필수): 추가할 아이템 UUID
                    - `quantity` (필수): 수량 (최소 1)

                    **Response:**
                    - `cartItemUuid`: 카트 아이템 UUID
                    - `itemUuid`: 아이템 UUID
                    - `itemName`: 아이템 이름
                    - `quantity`: 수량
                    - `price`: 단가 (코인)

                    동일 아이템을 다시 추가하면 수량이 누적됩니다.
                    ETC 아이템은 `POST /api/trade/transactions`를 이용하세요.
                    """
    )
    public ApiResponse<CartItemResponse> addToCart(
            @Valid @RequestBody CartAddRequest req,
            Authentication auth
    ) {
        if (auth == null) throw new SecurityException("Unauthenticated");
        return ApiResponse.success(cartService.addToCart(auth.getName(), req));
    }

    @GetMapping("/cart")
    @Operation(summary = "내 카트 조회", description = "현재 로그인한 사용자의 카트 목록을 조회합니다.")
    public ApiResponse<List<CartItemResponse>> getCart(Authentication auth) {
        if (auth == null) throw new SecurityException("Unauthenticated");
        return ApiResponse.success(cartService.getCart(auth.getName()));
    }

    @PatchMapping("/cart/{cartItemUuid}")
    @Operation(
            summary = "카트 수량 변경",
            description = """
                    카트 아이템의 수량을 변경합니다.

                    **Path Variable:**
                    - `cartItemUuid` (필수): 카트 아이템 UUID

                    **Request Body:**
                    - `quantity` (필수): 변경할 수량 (최소 1)

                    **Response:** 변경된 카트 아이템 정보

                    수량을 0으로 줄이려면 `DELETE /api/trade/cart/{cartItemUuid}`를 사용하세요.
                    """
    )
    public ApiResponse<CartItemResponse> updateCartItem(
            @Parameter(description = "카트 아이템 UUID") @PathVariable String cartItemUuid,
            @Valid @RequestBody CartUpdateRequest req,
            Authentication auth
    ) {
        if (auth == null) throw new SecurityException("Unauthenticated");
        return ApiResponse.success(cartService.updateQuantity(auth.getName(), cartItemUuid, req));
    }

    @DeleteMapping("/cart/{cartItemUuid}")
    @Operation(summary = "카트 아이템 삭제", description = "카트에서 특정 아이템을 제거합니다.")
    public ApiResponse<Void> removeCartItem(
            @Parameter(description = "카트 아이템 UUID") @PathVariable String cartItemUuid,
            Authentication auth
    ) {
        if (auth == null) throw new SecurityException("Unauthenticated");
        cartService.removeFromCart(auth.getName(), cartItemUuid);
        return ApiResponse.success();
    }

    @PostMapping("/cart/checkout")
    @Operation(
            summary = "카트 일괄 결제",
            description = "카트의 모든 FOOD 아이템을 코인으로 일괄 결제합니다.\n\n"
            + "재고 부족 또는 비활성 아이템이 하나라도 있으면 전체 결제가 취소됩니다.\n\n"
            + "결제 성공 시 카트가 비워집니다."
    )
    public ApiResponse<CartCheckoutResponse> checkout(Authentication auth) {
        if (auth == null) throw new SecurityException("Unauthenticated");
        return ApiResponse.success(cartService.checkout(auth.getName()));
    }

    @PatchMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(
            summary = "거래 아이템 수정 (관리자/선생님)",
            description = """
                    아이템 정보를 수정합니다. ADMIN/TEACHER만 가능합니다.
                    포함된 필드만 업데이트됩니다.

                    **Path Variable:**
                    - `itemId` (필수): 수정할 아이템 UUID

                    **Request Body (모두 선택):**
                    - `name`: 아이템 이름
                    - `description`: 아이템 설명
                    - `price`: 가격
                    - `active`: 판매 활성화 여부
                    - `imageUrl`: 이미지 URL

                    **Response:** 수정된 아이템 정보
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN/TEACHER만 가능)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "아이템을 찾을 수 없음")
            }
    )
    public ApiResponse<TradeItemResponse> updateItem(
            @Parameter(description = "수정할 아이템 UUID") @PathVariable("itemId") String itemId,
            @RequestBody TradeItemUpdateRequest req
    ) {
        TradeItem item = tradeService.updateItem(itemId, req);
        return ApiResponse.success(TradeItemResponse.from(item));
    }

    @PostMapping("/orders/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(
            summary = "구매 취소/환불 (관리자/선생님)",
            description = """
                    특정 구매 주문을 취소하고 결제 수단(코인/포인트)을 환불합니다.
                    ADMIN/TEACHER만 가능합니다.

                    **Path Variable:**
                    - `orderId` (필수): 취소할 주문의 log_uuid

                    **Response:**
                    - `logUuid`: 취소된 주문 UUID
                    - `itemName`: 아이템 이름
                    - `quantity`: 취소 수량
                    - `refundAmount`: 환불 금액 (코인 또는 포인트)
                    - `currency`: 결제 수단 (COIN/POINT)
                    - `balanceAfter`: 환불 후 잔액
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "취소/환불 성공"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 취소된 주문"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN/TEACHER만 가능)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음")
            }
    )
    public ApiResponse<TradeOrderCancelResponse> cancelOrder(
            @Parameter(description = "취소할 주문 UUID (log_uuid)") @PathVariable("orderId") String orderId,
            Authentication auth
    ) {
        if (auth == null) throw new SecurityException("Unauthenticated");
        TradeOrderCancelResponse result = tradeService.cancelOrder(orderId, auth.getName());
        return ApiResponse.success(result);
    }

    @GetMapping("/orders/stats")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(
            summary = "구매 내역 통계 (관리자/선생님)",
            description = """
                    전체 구매 주문 통계를 반환합니다. ADMIN/TEACHER만 가능합니다.

                    **Response:**
                    - `totalOrders`: 전체 주문 수
                    - `cancelledOrders`: 취소된 주문 수
                    - `activeOrders`: 유효 주문 수
                    - `totalCoinsSpent`: 코인 결제 총액
                    - `totalPointsSpent`: 포인트 결제 총액
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "통계 조회 성공"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN/TEACHER만 가능)")
            }
    )
    public ApiResponse<TradeOrderStatsResponse> getOrderStats() {
        return ApiResponse.success(tradeService.getOrderStats());
    }

    @GetMapping("/items/stats")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(
            summary = "상점 판매 통계 (관리자/선생님)",
            description = """
                    아이템별 판매 수량·매출 통계를 반환합니다. ADMIN/TEACHER만 가능합니다.

                    **Response:** 아이템별 itemUuid, itemName, totalQuantitySold, totalRevenue, orderCount 목록
                    (매출액 내림차순 정렬)
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "통계 조회 성공"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN/TEACHER만 가능)")
            }
    )
    public ApiResponse<List<TradeItemStatsResponse>> getItemStats() {
        return ApiResponse.success(tradeService.getItemStats());
    }

    @PostMapping("/items/{uuid}/stock")
    @Operation(
            summary = "재고 충전",
            description = """
                    아이템 재고를 충전합니다. ADMIN 또는 TEACHER만 가능합니다.

                    **Request Body:**
                    - `itemUuid` (필수): 재고를 충전할 아이템 UUID
                    - `amount` (필수): 충전할 수량

                    **Response:** 성공 응답 (data: null)
                    """
    )
    public ApiResponse<Void> restock(
            @RequestBody RestockRequest req,
            Authentication auth
    ) {
        tradeService.restockItem(req.getItemUuid(), req.getAmount(), auth.getName());
        return ApiResponse.success();
    }
}
