package com.ada.proj.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.UserInventoryResponse;
import com.ada.proj.exception.ForbiddenException;
import com.ada.proj.service.InventoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/users/{uuid}/inventory")
@Tag(name = "인벤토리", description = "사용자 인벤토리 조회 및 배너 적용 API")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    private void ensureSelfOrAdmin(Authentication auth, String targetUuid) {
        if (auth == null) throw new ForbiddenException("인증이 필요합니다.");
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !auth.getName().equals(targetUuid)) {
            throw new ForbiddenException("본인의 인벤토리만 조회할 수 있습니다.");
        }
    }

    @GetMapping
    @Operation(
            summary = "인벤토리 조회",
            description = """
                    사용자가 보유한 아이템 목록을 조회합니다. 본인 또는 ADMIN만 조회 가능합니다.

                    **Path Variable:**
                    - `uuid` (필수): 대상 사용자 UUID

                    **Response:** 인벤토리 아이템 목록 (획득 일시 내림차순)
                    """
    )
    public ResponseEntity<ApiResponse<List<UserInventoryResponse>>> getInventory(
            @Parameter(description = "대상 사용자 UUID") @PathVariable String uuid,
            Authentication auth) {

        ensureSelfOrAdmin(auth, uuid);
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getInventory(uuid)));
    }

    @PatchMapping("/{inventoryUuid}/apply-banner")
    @Operation(
            summary = "인벤토리 배너 적용",
            description = """
                    인벤토리에 보유한 배너 아이템을 프로필 배너로 설정합니다. 본인 또는 ADMIN만 가능합니다.

                    **Path Variable:**
                    - `uuid` (필수): 대상 사용자 UUID
                    - `inventoryUuid` (필수): 적용할 인벤토리 아이템 UUID

                    **Response:** 성공 메시지 반환

                    BANNER 소분류 아이템만 적용 가능합니다.
                    """
    )
    public ResponseEntity<ApiResponse<Void>> applyBanner(
            @Parameter(description = "대상 사용자 UUID") @PathVariable String uuid,
            @Parameter(description = "인벤토리 아이템 UUID") @PathVariable String inventoryUuid,
            Authentication auth) {

        ensureSelfOrAdmin(auth, uuid);
        inventoryService.applyBanner(uuid, inventoryUuid);
        return ResponseEntity.ok(ApiResponse.okMessage("banner applied"));
    }
}
