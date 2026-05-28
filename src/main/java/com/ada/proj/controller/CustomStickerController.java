package com.ada.proj.controller;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.CustomStickerResponse;
import com.ada.proj.dto.CustomStickerReviewRequest;
import com.ada.proj.dto.CustomStickerSubmitRequest;
import com.ada.proj.dto.PageResponse;
import com.ada.proj.enums.CustomStickerStatus;
import com.ada.proj.service.CustomStickerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/custom-stickers")
@RequiredArgsConstructor
@Validated
@Tag(name = "커스텀 스티커", description = "사용자 제작 스티커 등록/검수 API")
public class CustomStickerController {

    private final CustomStickerService customStickerService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "커스텀 스티커 등록",
            description = """
                    로그인 사용자가 커스텀 스티커를 등록합니다.

                    등록 시 **1500 포인트**가 차감되며, 등록된 스티커는 PENDING(검수 대기) 상태가 됩니다.
                    선생님 또는 관리자가 검수 후 승인하면 커뮤니티 등에서 사용할 수 있습니다.

                    **Request (multipart/form-data):**
                    - `file` (필수): 스티커 이미지 파일 (jpeg, png, gif, webp / 최대 15MB)
                    - `name` (필수): 스티커 이름
                    - `description` (선택): 스티커 설명
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<CustomStickerResponse> submit(
            @Parameter(description = "스티커 이미지 파일") @RequestPart("file") MultipartFile file,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            Authentication auth
    ) {
        if (auth == null) throw new SecurityException("Unauthenticated");
        CustomStickerSubmitRequest req = new CustomStickerSubmitRequest();
        req.setName(name);
        req.setDescription(description);
        return ApiResponse.success(
                CustomStickerResponse.from(customStickerService.submit(auth.getName(), req, file))
        );
    }

    @GetMapping("/me")
    @Operation(
            summary = "내 커스텀 스티커 목록 조회",
            description = """
                    로그인 사용자가 자신이 등록한 커스텀 스티커 목록을 조회합니다.

                    **Query Parameters:**
                    - `status` (선택): PENDING | APPROVED | REJECTED (미입력 시 전체 조회)
                    - `page` (기본값 0): 페이지 번호
                    - `size` (기본값 20): 페이지 크기
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<PageResponse<CustomStickerResponse>> getMyStickers(
            @RequestParam(required = false) CustomStickerStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth
    ) {
        if (auth == null) throw new SecurityException("Unauthenticated");
        var result = customStickerService.getMyStickers(auth.getName(), status, page, size)
                .map(CustomStickerResponse::from);
        return ApiResponse.success(new PageResponse<>(
                result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), result.getContent()
        ));
    }

    @GetMapping("/approved")
    @Operation(
            summary = "승인된 커스텀 스티커 목록 조회",
            description = "커뮤니티 등에서 사용 가능한 승인된 커스텀 스티커 목록을 조회합니다."
    )
    public ApiResponse<PageResponse<CustomStickerResponse>> getApprovedStickers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var result = customStickerService.getApprovedStickers(page, size)
                .map(CustomStickerResponse::from);
        return ApiResponse.success(new PageResponse<>(
                result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), result.getContent()
        ));
    }

    @GetMapping("/{stickerUuid}")
    @Operation(
            summary = "커스텀 스티커 단건 조회",
            description = "stickerUuid로 커스텀 스티커 상세 정보를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<CustomStickerResponse> getSticker(
            @Parameter(description = "조회할 스티커 UUID") @PathVariable String stickerUuid,
            Authentication auth
    ) {
        if (auth == null) throw new SecurityException("Unauthenticated");
        var sticker = customStickerService.getSticker(stickerUuid);

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isTeacher = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));
        boolean isOwner = sticker.getUserUuid().equals(auth.getName());

        if (!isAdmin && !isTeacher && !isOwner) {
            throw new SecurityException("Forbidden");
        }
        return ApiResponse.success(CustomStickerResponse.from(sticker));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(
            summary = "검수 대기 스티커 목록 조회 (ADMIN/TEACHER)",
            description = "검수 대기(PENDING) 상태의 커스텀 스티커 목록을 등록일 오름차순으로 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<PageResponse<CustomStickerResponse>> getPendingStickers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var result = customStickerService.getPendingStickers(page, size)
                .map(CustomStickerResponse::from);
        return ApiResponse.success(new PageResponse<>(
                result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), result.getContent()
        ));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(
            summary = "전체 커스텀 스티커 목록 조회 (ADMIN/TEACHER)",
            description = "모든 상태의 커스텀 스티커 전체 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<PageResponse<CustomStickerResponse>> getAllStickers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var result = customStickerService.getAllStickers(page, size)
                .map(CustomStickerResponse::from);
        return ApiResponse.success(new PageResponse<>(
                result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages(), result.getContent()
        ));
    }

    @PostMapping("/{stickerUuid}/review")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(
            summary = "커스텀 스티커 검수 (ADMIN/TEACHER)",
            description = """
                    PENDING 상태의 커스텀 스티커를 승인하거나 반려합니다.

                    **승인(APPROVED):** 스티커 상태가 APPROVED로 변경되어 커뮤니티 등에서 사용 가능해집니다.

                    **반려(REJECTED):** 스티커 상태가 REJECTED로 변경됩니다.
                    - `rejectionReason` 필드에 반려 사유를 반드시 입력해야 합니다.
                    - `refundOnReject=true` (기본값)이면 등록 수수료가 포인트로 환불됩니다.

                    **Request Body:**
                    - `status` (필수): APPROVED | REJECTED
                    - `rejectionReason` (REJECTED 시 필수): 반려 사유
                    - `refundOnReject` (선택, 기본 true): 반려 시 포인트 환불 여부
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<CustomStickerResponse> review(
            @Parameter(description = "검수할 스티커 UUID") @PathVariable String stickerUuid,
            @Valid @RequestBody CustomStickerReviewRequest req,
            Authentication auth
    ) {
        if (auth == null) throw new SecurityException("Unauthenticated");
        return ApiResponse.success(
                CustomStickerResponse.from(customStickerService.review(stickerUuid, req, auth.getName()))
        );
    }
}
