package com.ada.proj.controller;

import com.ada.proj.dto.*;
import com.ada.proj.enums.StudyGroupCategory;
import com.ada.proj.service.AdminPostService;
import com.ada.proj.service.AdminStatsService;
import com.ada.proj.service.AutoIncrementMaintenanceService;
import com.ada.proj.service.NotificationService;
import com.ada.proj.service.StudyGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@Tag(name = "관리자 도구", description = "ADMIN/TEACHER 전용 — 게시글 관리, 알림 발송, 그룹 관리, 통계 대시보드 등")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AutoIncrementMaintenanceService aiService;
    private final AdminPostService adminPostService;
    private final AdminStatsService adminStatsService;
    private final NotificationService notificationService;
    private final StudyGroupService studyGroupService;

    // ── AUTO_INCREMENT 재정렬 ──────────────────────────────────────────────────

    @PostMapping("/resequence/{table}")
    @Operation(
            summary = "자동 증가 재정렬 (관리자/선생님)",
            description = """
                    특정 테이블의 AUTO_INCREMENT 값을 재정렬합니다. ADMIN/TEACHER 전용입니다.

                    **Path Variable:**
                    - `table` (필수): 재정렬할 테이블 이름 (예: posts, comments)

                    **Response:** 성공 메시지 반환 (재정렬 시작 알림)
                    """,
            responses = {
                @ApiResponse(responseCode = "200", description = "재정렬 시작"),
                @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
                @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN/TEACHER만 가능)", content = @Content)
            }
    )
    public ResponseEntity<com.ada.proj.dto.ApiResponse<?>> resequenceTable(
            @Parameter(description = "재정렬할 테이블 이름") @PathVariable("table") String table,
            Authentication auth) {
        requireAdminOrTeacher(auth);
        aiService.triggerResequencePrimary(table);
        return ResponseEntity.ok(com.ada.proj.dto.ApiResponse.success("resequence started for: " + table));
    }

    // ── 게시글 관리 ────────────────────────────────────────────────────────────

    @GetMapping("/posts")
    @Operation(
            summary = "전체 게시글 관리 목록 (관리자/선생님)",
            description = """
                    전체 게시글 목록을 관리자 관점으로 조회합니다. ADMIN/TEACHER 전용입니다.

                    **Query Parameters:**
                    - `writerUuid` (선택): 특정 작성자로 필터
                    - `q` (선택): 제목 검색어
                    - `page` (선택): 페이지 번호 (기본값: 0)
                    - `size` (선택): 페이지 크기 (기본값: 20)

                    **Response:** 페이징된 게시글 목록 (postUuid, writerUuid, seq, title, writer, writedAt, likes, views, comments, boardType, communityCategory, thumbnailImage)
                    """,
            responses = {
                @ApiResponse(responseCode = "200", description = "조회 성공"),
                @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
                @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN/TEACHER만 가능)", content = @Content)
            }
    )
    public ResponseEntity<com.ada.proj.dto.ApiResponse<PageResponse<AdminPostSummaryResponse>>> getAdminPosts(
            @Parameter(description = "작성자 UUID 필터") @RequestParam(required = false) String writerUuid,
            @Parameter(description = "제목 검색어") @RequestParam(required = false, name = "q") String query,
            @Parameter(description = "페이지 번호 (0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        requireAdminOrTeacher(auth);
        return ResponseEntity.ok(com.ada.proj.dto.ApiResponse.success(
                adminPostService.getAdminPosts(writerUuid, query, page, size)));
    }

    @DeleteMapping("/posts/users/{uuid}")
    @Operation(
            summary = "특정 유저 게시글 일괄 삭제 (관리자/선생님)",
            description = """
                    특정 사용자의 게시글을 모두 삭제합니다. ADMIN/TEACHER 전용입니다.
                    댓글 좋아요 → 댓글 → 게시글 좋아요/북마크 → 게시글 순서로 안전하게 삭제합니다.

                    **Path Variable:**
                    - `uuid` (필수): 게시글을 삭제할 사용자 UUID

                    **Response:** 삭제된 게시글 수 메시지
                    """,
            responses = {
                @ApiResponse(responseCode = "200", description = "삭제 성공"),
                @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
                @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN/TEACHER만 가능)", content = @Content)
            }
    )
    public ResponseEntity<com.ada.proj.dto.ApiResponse<Void>> deleteUserPosts(
            @Parameter(description = "대상 사용자 UUID") @PathVariable String uuid,
            Authentication auth) {
        requireAdminOrTeacher(auth);
        int count = adminPostService.deletePostsByUser(uuid);
        return ResponseEntity.ok(com.ada.proj.dto.ApiResponse.successMessage("게시글 " + count + "개가 삭제되었습니다."));
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(
            summary = "댓글 강제 삭제 (관리자/선생님)",
            description = """
                    특정 댓글을 강제로 삭제합니다. ADMIN/TEACHER 전용입니다.
                    해당 댓글의 좋아요 및 자식 댓글도 함께 삭제됩니다.

                    **Path Variable:**
                    - `commentId` (필수): 삭제할 댓글 ID (숫자)

                    **Response:** 성공 메시지 반환
                    """,
            responses = {
                @ApiResponse(responseCode = "200", description = "삭제 성공"),
                @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
                @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN/TEACHER만 가능)", content = @Content),
                @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음", content = @Content)
            }
    )
    public ResponseEntity<com.ada.proj.dto.ApiResponse<Void>> forceDeleteComment(
            @Parameter(description = "삭제할 댓글 ID") @PathVariable Long commentId,
            Authentication auth) {
        requireAdminOrTeacher(auth);
        adminPostService.forceDeleteComment(commentId);
        return ResponseEntity.ok(com.ada.proj.dto.ApiResponse.okMessage("comment deleted"));
    }

    // ── 알림 관리 ──────────────────────────────────────────────────────────────

    @PostMapping("/notifications/send")
    @Operation(
            summary = "전체 공지 알림 발송 (관리자/선생님)",
            description = """
                    특정 역할(또는 전체)의 사용자에게 공지 알림을 발송합니다. ADMIN/TEACHER 전용입니다.

                    **Request Body:**
                    - `title` (필수): 알림 제목
                    - `message` (필수): 알림 내용
                    - `targetRole` (선택): 대상 역할 (STUDENT | TEACHER | ADMIN, null이면 전체 발송)
                    - `postUuid` (선택): 관련 게시글 UUID

                    **Response:** 발송된 인원 수 메시지
                    """,
            responses = {
                @ApiResponse(responseCode = "200", description = "발송 성공"),
                @ApiResponse(responseCode = "400", description = "필수 필드 누락", content = @Content),
                @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
                @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN/TEACHER만 가능)", content = @Content)
            }
    )
    public ResponseEntity<com.ada.proj.dto.ApiResponse<Void>> sendNotification(
            @Valid @RequestBody AdminNotificationSendRequest req,
            Authentication auth) {
        requireAdminOrTeacher(auth);
        int count = notificationService.sendGlobal(req, auth.getName());
        return ResponseEntity.ok(com.ada.proj.dto.ApiResponse.successMessage("알림 " + count + "명에게 발송되었습니다."));
    }

    @PostMapping("/notifications/users/{uuid}/send")
    @Operation(
            summary = "특정 사용자에게 알림 직접 발송 (관리자/선생님)",
            description = """
                    특정 사용자 한 명에게 알림을 직접 발송합니다. ADMIN/TEACHER 전용입니다.

                    **Path Variable:**
                    - `uuid` (필수): 알림을 받을 사용자 UUID

                    **Request Body:**
                    - `title` (필수): 알림 제목
                    - `message` (필수): 알림 내용
                    - `postUuid` (선택): 관련 게시글 UUID
                    - `targetRole`: 이 API에서는 사용하지 않습니다.

                    **Response:** 성공 메시지
                    """,
            responses = {
                @ApiResponse(responseCode = "200", description = "발송 성공"),
                @ApiResponse(responseCode = "400", description = "필수 필드 누락", content = @Content),
                @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
                @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN/TEACHER만 가능)", content = @Content),
                @ApiResponse(responseCode = "404", description = "대상 사용자를 찾을 수 없음", content = @Content)
            }
    )
    public ResponseEntity<com.ada.proj.dto.ApiResponse<Void>> sendDirectNotification(
            @Parameter(description = "알림을 받을 사용자 UUID") @PathVariable String uuid,
            @Valid @RequestBody AdminNotificationSendRequest req,
            Authentication auth) {
        requireAdminOrTeacher(auth);
        notificationService.sendDirect(uuid, req, auth.getName());
        return ResponseEntity.ok(com.ada.proj.dto.ApiResponse.successMessage("알림이 발송되었습니다."));
    }

    @GetMapping("/notifications/history")
    @Operation(
            summary = "알림 발송 이력 조회 (관리자/선생님)",
            description = """
                    관리자/선생님이 발송한 공지 알림 이력을 조회합니다. ADMIN/TEACHER 전용입니다.

                    **Query Parameters:**
                    - `page` (선택): 페이지 번호 (기본값: 0)
                    - `size` (선택): 페이지 크기 (기본값: 20)

                    **Response:** 페이징된 알림 목록 (id, type, title, message, postUuid, createdAt, senderUuid)
                    """,
            responses = {
                @ApiResponse(responseCode = "200", description = "조회 성공"),
                @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
                @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN/TEACHER만 가능)", content = @Content)
            }
    )
    public ResponseEntity<com.ada.proj.dto.ApiResponse<PageResponse<NotificationResponse>>> getNotificationHistory(
            @Parameter(description = "페이지 번호 (0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        requireAdminOrTeacher(auth);
        return ResponseEntity.ok(com.ada.proj.dto.ApiResponse.success(notificationService.getAdminHistory(page, size)));
    }

    // ── 스터디 그룹 관리 ───────────────────────────────────────────────────────

    @GetMapping("/groups")
    @Operation(
            summary = "전체 그룹 관리 목록 (관리자/선생님)",
            description = """
                    전체 스터디 그룹 목록을 관리자 관점으로 조회합니다. ADMIN/TEACHER 전용입니다.

                    **Query Parameters:**
                    - `page` (선택): 페이지 번호 (기본값: 0)
                    - `size` (선택): 페이지 크기 (기본값: 20)

                    **Response:** 페이징된 그룹 목록 (groupUuid, name, category, visibility, status, capacity, ownerUuid, memberCount, createdAt)
                    """,
            responses = {
                @ApiResponse(responseCode = "200", description = "조회 성공"),
                @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
                @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN/TEACHER만 가능)", content = @Content)
            }
    )
    public ResponseEntity<com.ada.proj.dto.ApiResponse<PageResponse<AdminGroupSummaryResponse>>> getAdminGroups(
            @Parameter(description = "페이지 번호 (0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        requireAdminOrTeacher(auth);
        return ResponseEntity.ok(com.ada.proj.dto.ApiResponse.success(studyGroupService.getAdminGroups(page, size)));
    }

    @GetMapping("/groups/category/{id}")
    @Operation(
            summary = "카테고리별 그룹 관리 목록 (관리자/선생님)",
            description = """
                    특정 카테고리의 스터디 그룹 목록을 관리자 관점으로 조회합니다. ADMIN/TEACHER 전용입니다.

                    **Path Variable:**
                    - `id` (필수): 조회할 카테고리 (LANGUAGE_STUDY | PROJECT)

                    **Query Parameters:**
                    - `page` (선택): 페이지 번호 (기본값: 0)
                    - `size` (선택): 페이지 크기 (기본값: 20)

                    **Response:** 페이징된 그룹 목록
                    """,
            responses = {
                @ApiResponse(responseCode = "200", description = "조회 성공"),
                @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
                @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN/TEACHER만 가능)", content = @Content)
            }
    )
    public ResponseEntity<com.ada.proj.dto.ApiResponse<PageResponse<AdminGroupSummaryResponse>>> getAdminGroupsByCategory(
            @Parameter(description = "카테고리 (LANGUAGE_STUDY | PROJECT)") @PathVariable StudyGroupCategory id,
            @Parameter(description = "페이지 번호 (0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        requireAdminOrTeacher(auth);
        return ResponseEntity.ok(com.ada.proj.dto.ApiResponse.success(
                studyGroupService.getAdminGroupsByCategory(id, page, size)));
    }

    @PostMapping("/groups/{groupId}/dissolve")
    @Operation(
            summary = "그룹 강제 해산 (관리자/선생님)",
            description = """
                    스터디 그룹을 강제로 해산합니다.
                    그룹 상태를 CLOSED로 변경하고 모든 멤버를 제거합니다.
                    ADMIN/TEACHER 전용입니다.

                    **Path Variable:**
                    - `groupId` (필수): 해산할 그룹 UUID

                    **Response:** 성공 메시지 반환
                    """,
            responses = {
                @ApiResponse(responseCode = "200", description = "해산 성공"),
                @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
                @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN/TEACHER만 가능)", content = @Content),
                @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음", content = @Content)
            }
    )
    public ResponseEntity<com.ada.proj.dto.ApiResponse<Void>> dissolveGroup(
            @Parameter(description = "그룹 UUID") @PathVariable String groupId,
            Authentication auth) {
        requireAdminOrTeacher(auth);
        studyGroupService.dissolveGroup(groupId);
        return ResponseEntity.ok(com.ada.proj.dto.ApiResponse.okMessage("group dissolved"));
    }

    @DeleteMapping("/groups/{groupId}/members/{uuid}")
    @Operation(
            summary = "그룹 멤버 강제 탈퇴 (관리자/선생님)",
            description = """
                    특정 스터디 그룹에서 특정 멤버를 강제로 탈퇴시킵니다.
                    리더는 강제 탈퇴할 수 없습니다 (먼저 리더 위임 필요).
                    ADMIN/TEACHER 전용입니다.

                    **Path Variables:**
                    - `groupId` (필수): 그룹 UUID
                    - `uuid` (필수): 탈퇴시킬 멤버의 사용자 UUID

                    **Response:** 성공 메시지 반환
                    """,
            responses = {
                @ApiResponse(responseCode = "200", description = "강제 탈퇴 성공"),
                @ApiResponse(responseCode = "400", description = "리더 탈퇴 불가", content = @Content),
                @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
                @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN/TEACHER만 가능)", content = @Content),
                @ApiResponse(responseCode = "404", description = "그룹 또는 멤버를 찾을 수 없음", content = @Content)
            }
    )
    public ResponseEntity<com.ada.proj.dto.ApiResponse<Void>> forceRemoveMember(
            @Parameter(description = "그룹 UUID") @PathVariable String groupId,
            @Parameter(description = "탈퇴시킬 사용자 UUID") @PathVariable String uuid,
            Authentication auth) {
        requireAdminOrTeacher(auth);
        studyGroupService.forceRemoveMember(groupId, uuid);
        return ResponseEntity.ok(com.ada.proj.dto.ApiResponse.okMessage("member removed"));
    }

    // ── 통계 대시보드 ──────────────────────────────────────────────────────────

    @GetMapping("/stats/summary")
    @Operation(
            summary = "전체 현황 요약 (관리자/선생님)",
            description = """
                    플랫폼 전체 현황을 요약하여 반환합니다. ADMIN/TEACHER 전용입니다.

                    **Response:**
                    - `totalUsers`: 전체 사용자 수
                    - `totalStudents`: 학생 수
                    - `totalTeachers`: 선생님 수
                    - `totalAdmins`: 관리자 수
                    - `totalPosts`: 전체 게시글 수
                    - `totalComments`: 전체 댓글 수
                    - `activeBans`: 현재 활성 제재 수
                    - `totalGroups`: 전체 스터디 그룹 수
                    - `totalTradeOrders`: 전체 거래 주문 수
                    - `totalNotifications`: 전체 알림 수
                    """,
            responses = {
                @ApiResponse(responseCode = "200", description = "조회 성공"),
                @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
                @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN/TEACHER만 가능)", content = @Content)
            }
    )
    public ResponseEntity<com.ada.proj.dto.ApiResponse<AdminStatsSummaryResponse>> getSummary(Authentication auth) {
        requireAdminOrTeacher(auth);
        return ResponseEntity.ok(com.ada.proj.dto.ApiResponse.success(adminStatsService.getSummary()));
    }

    // ── 권한 검증 헬퍼 ────────────────────────────────────────────────────────

    private void requireAdminOrTeacher(Authentication auth) {
        if (auth == null) {
            throw new org.springframework.security.access.AccessDeniedException("로그인이 필요합니다.");
        }
        boolean ok = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ADMIN")
                        || a.getAuthority().equals("ROLE_TEACHER")
                        || a.getAuthority().equals("TEACHER"));
        if (!ok) {
            throw new org.springframework.security.access.AccessDeniedException("관리자 또는 선생님 권한이 필요합니다.");
        }
    }
}
