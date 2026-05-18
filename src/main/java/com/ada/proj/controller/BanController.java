// src/main/java/com/ada/proj/controller/BanController.java
package com.ada.proj.controller;

import com.ada.proj.dto.BanInfoResponse;
import com.ada.proj.dto.BanRequest;
import com.ada.proj.dto.BanResponse;
import com.ada.proj.dto.BanStatsResponse;
import com.ada.proj.dto.PageResponse;
import com.ada.proj.service.BanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "제재 관리", description = "유저 제재 관리 API (ADMIN/TEACHER 전용)")
@SecurityRequirement(name = "bearerAuth")
public class BanController {

    private final BanService banService;

    @Operation(
            summary = "제재 생성",
            description = "운영자가 특정 유저에게 기간 제재를 부여합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = BanRequest.class),
                            examples = @ExampleObject(
                                    value = """
                        {
                          \"targetUuid\": \"6d13ec47-e585-4cf7-b6dd-7fd8c97d3a2e\",
                          \"reason\": \"욕설 및 비방\",
                          \"durationValue\": 3,
                          \"durationUnit\": \"DAYS\"
                        }
                        """
                            )
                    )
            ),
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description = "제재 적용 성공",
                        content = @Content(
                                schema = @Schema(implementation = BanResponse.class),
                                examples = @ExampleObject(
                                        value = """
                            {
                              \"targetUuid\": \"6d13ec47-e585-4cf7-b6dd-7fd8c97d3a2e\",
                              \"reason\": \"욕설 및 비방\",
                              \"durationValue\": 3,
                              \"durationUnit\": \"DAYS\",
                              \"startsAtKst\": \"2025-12-01T10:00:00\",
                              \"expiresAtKst\": \"2025-12-04T10:00:00\",
                              \"expiresAtUtc\": \"2025-12-04T01:00:00\"
                            }
                            """
                                )
                        )
                ),
                @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                @ApiResponse(responseCode = "401", description = "JWT 인증 실패"),
                @ApiResponse(responseCode = "403", description = "권한 없음"),
                @ApiResponse(responseCode = "409", description = "이미 제재 중")
            }
    )
    @PostMapping("/bans")
    public ResponseEntity<com.ada.proj.dto.ApiResponse<BanResponse>> createBan(@Valid @RequestBody BanRequest request) {
        BanResponse response = banService.createBan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(com.ada.proj.dto.ApiResponse.success(response));
    }

    @Operation(
            summary = "제재 목록 조회",
            description = """
                    제재 내역을 페이징으로 조회합니다. ADMIN/TEACHER만 가능합니다.

                    **Query Parameters:**
                    - `page` (선택): 페이지 번호, 0부터 시작 (기본값: 0)
                    - `size` (선택): 페이지당 데이터 수 (기본값: 20)
                    - `activeOnly` (선택): true이면 현재 활성 제재만 반환 (기본값: true)

                    **Response:** 페이징된 제재 목록 (page, size, totalElements, totalPages, content)
                    - `content[].targetUuid`: 제재 대상 UUID
                    - `content[].reason`: 제재 사유
                    - `content[].startsAtKst`: 제재 시작 시각 (KST)
                    - `content[].expiresAtKst`: 제재 만료 시각 (KST)
                    """
    )
    @GetMapping("/bans")
    public com.ada.proj.dto.ApiResponse<PageResponse<BanInfoResponse>> getBans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        return com.ada.proj.dto.ApiResponse.success(banService.getBanList(activeOnly, page, size));
    }

    @Operation(
            summary = "특정 유저 제재 내역",
            description = """
                    특정 사용자의 제재 내역을 조회합니다. ADMIN/TEACHER만 가능합니다.

                    **Path Variable:**
                    - `userUuid` (필수): 조회할 사용자 UUID

                    **Query Parameters:**
                    - `activeOnly` (선택): true이면 현재 진행 중인 제재만 반환 (기본값: true)

                    **Response:** 제재 목록 배열 (targetUuid, reason, startsAtKst, expiresAtKst 등)
                    """
    )
    @GetMapping("/bans/users/{userUuid}")
    public com.ada.proj.dto.ApiResponse<List<BanInfoResponse>> getBanHistory(
            @PathVariable String userUuid,
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        return com.ada.proj.dto.ApiResponse.success(banService.getBanHistoryForUser(userUuid, activeOnly));
    }

    @Operation(
            summary = "유저 제재 해제 (UUID)",
            description = """
                    사용자 UUID로 현재 활성 제재를 해제합니다. ADMIN/TEACHER만 가능합니다.

                    **Path Variable:**
                    - `userUuid` (필수): 제재를 해제할 사용자 UUID

                    **Request Body:** 없음

                    **Response:** 성공 메시지 반환
                    """
    )
    @PostMapping("/bans/{userUuid}/release")
    public com.ada.proj.dto.ApiResponse<Void> releaseBan(@PathVariable String userUuid) {
        banService.releaseBan(userUuid);
        return com.ada.proj.dto.ApiResponse.okMessage("제재가 해제되었습니다.");
    }

    @Operation(
            summary = "제재 해제 (banId)",
            description = """
                    제재 ID로 특정 제재를 직접 해제합니다. ADMIN/TEACHER만 가능합니다.

                    **Path Variable:**
                    - `banId` (필수): 해제할 제재의 숫자 ID

                    **Request Body:** 없음

                    **Response:** 성공 메시지 반환
                    """
    )
    @PostMapping("/bans/{banId}/release/manual")
    public com.ada.proj.dto.ApiResponse<Void> releaseManual(@PathVariable Long banId) {
        banService.releaseBanManual(banId);
        return com.ada.proj.dto.ApiResponse.success();
    }

    @Operation(
            summary = "제재 사유 통계 (관리자/선생님)",
            description = """
                    제재 사유별 건수 통계를 반환합니다. ADMIN/TEACHER만 가능합니다.

                    **Response:** 사유(reason) + 건수(count) 목록, 건수 내림차순 정렬
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth"),
            responses = {
                @ApiResponse(responseCode = "200", description = "통계 조회 성공"),
                @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
                @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN/TEACHER만 가능)", content = @Content)
            }
    )
    @GetMapping("/bans/stats")
    public com.ada.proj.dto.ApiResponse<List<BanStatsResponse>> getBanStats() {
        return com.ada.proj.dto.ApiResponse.success(banService.getBanStats());
    }
}
