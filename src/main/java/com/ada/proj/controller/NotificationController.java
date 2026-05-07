package com.ada.proj.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.NotificationResponse;
import com.ada.proj.dto.PageResponse;
import com.ada.proj.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
@Tag(name = "알림", description = "사용자 알림 목록 조회 및 읽음 처리 API")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(
            summary = "내 알림 목록 조회",
            description = """
                    로그인 사용자의 알림 목록을 최신순으로 조회합니다.

                    **Query Parameters:**
                    - `page` (선택): 페이지 번호, 0부터 시작
                    - `size` (선택): 페이지 크기

                    **Response:**
                    - `id`: 알림 ID
                    - `type`: 알림 타입. 현재 `POLL_ENDED` 지원
                    - `title`: 알림 제목
                    - `message`: 알림 내용
                    - `postUuid`: 관련 게시글 UUID
                    - `readAt`: 읽음 처리 시각
                    - `createdAt`: 알림 생성 시각
                    """
    )
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> list(
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.list(authentication, page, size)));
    }

    @PutMapping("/{id}/read")
    @Operation(
            summary = "알림 읽음 처리",
            description = """
                    내 알림을 읽음 처리합니다. 이미 읽은 알림이면 기존 `readAt` 값을 유지합니다.

                    **Path Variable:**
                    - `id` (필수): 알림 ID

                    **Response:** 성공 응답
                    """
    )
    public ResponseEntity<ApiResponse<Void>> markRead(
            @Parameter(description = "알림 ID", example = "1") @PathVariable Long id,
            Authentication authentication
    ) {
        notificationService.markRead(id, authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
