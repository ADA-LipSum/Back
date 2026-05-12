package com.ada.proj.controller;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.BannerCreateRequest;
import com.ada.proj.dto.BannerResponse;
import com.ada.proj.service.CommunityBannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/community/banners")
@RequiredArgsConstructor
@Tag(name = "커뮤니티 배너", description = "커뮤니티 상단 배너 조회/등록/수정/삭제 API")
public class CommunityBannerController {

    private final CommunityBannerService bannerService;

    @GetMapping
    @Operation(
            summary = "활성 배너 목록 조회",
            description = "활성화된 배너 목록을 `displayOrder` 오름차순으로 반환합니다. 로그인 없이 누구나 조회할 수 있습니다."
    )
    public ResponseEntity<ApiResponse<List<BannerResponse>>> getBanners() {
        return ResponseEntity.ok(ApiResponse.success(bannerService.getActiveBanners()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(
            summary = "전체 배너 목록 조회 (관리자/선생님)",
            description = "비활성 배너를 포함한 전체 목록을 반환합니다. ADMIN 또는 TEACHER 권한이 필요합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<List<BannerResponse>>> getAllBanners() {
        return ResponseEntity.ok(ApiResponse.success(bannerService.getAllBanners()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(
            summary = "배너 등록",
            description = """
                    커뮤니티 상단 배너를 등록합니다. ADMIN 또는 TEACHER 권한이 필요합니다.

                    **권장 이미지 크기:** 1247 × 320 px

                    - `imageUrl` (필수): 배너 이미지 URL
                    - `linkUrl` (선택): 클릭 시 이동할 URL
                    - `title` (선택): 배너 제목/설명
                    - `displayOrder` (선택, 기본 0): 노출 순서 — 낮을수록 앞에 표시
                    - `active` (선택, 기본 true): 노출 여부
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<BannerResponse>> createBanner(
            @Valid @RequestBody BannerCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(bannerService.createBanner(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(
            summary = "배너 수정",
            description = "배너를 수정합니다. ADMIN 또는 TEACHER 권한이 필요합니다. `null` 필드는 수정하지 않습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<BannerResponse>> updateBanner(
            @Parameter(description = "배너 ID", example = "1") @PathVariable Long id,
            @RequestBody BannerCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(bannerService.updateBanner(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(
            summary = "배너 삭제",
            description = "배너를 삭제합니다. ADMIN 또는 TEACHER 권한이 필요합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> deleteBanner(
            @Parameter(description = "배너 ID", example = "1") @PathVariable Long id
    ) {
        bannerService.deleteBanner(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
