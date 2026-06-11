package com.ada.proj.controller;

import com.ada.proj.dto.*;
import com.ada.proj.service.StudyGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/studies")
@Tag(name = "스터디 그룹", description = "스터디 그룹 생성/검색/가입/탈퇴/상태 변경 API")
public class StudyGroupController {

    private final StudyGroupService studyGroupService;

    @PostMapping("/groups")
    @Operation(
            summary = "그룹 생성",
            description = """
                    새 스터디 그룹을 생성합니다. 로그인이 필요합니다.

                    **Request Body:**
                    - `name` (필수): 그룹 이름
                    - `description` (선택): 그룹 설명
                    - `techTags` (선택): 기술 태그 문자열
                    - `visibility` (필수): 공개 여부 (PUBLIC | PRIVATE)
                    - `capacity` (필수): 최대 인원 수 (1~1000)

                    **Response:**
                    - `data`: 생성된 그룹 UUID

                    성공 시 HTTP 201을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody StudyGroupCreateRequest req,
            Authentication authentication) {
        String ownerUuid = authentication != null ? authentication.getName() : null;
        if (ownerUuid == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("UNAUTHORIZED", "인증이 필요합니다."));
        }
        String uuid = studyGroupService.create(java.util.Objects.requireNonNull(req), java.util.Objects.requireNonNull(ownerUuid));
        return ResponseEntity.status(201).body(ApiResponse.success(uuid));
    }

    @GetMapping("/groups/{uuid}")
    @Operation(
            summary = "그룹 상세 조회",
            description = """
                    스터디 그룹 상세 정보를 조회합니다.

                    **Path Variable:**
                    - `uuid` (필수): 그룹 UUID

                    **Response:**
                    - `uuid`: 그룹 UUID
                    - `name`: 그룹 이름
                    - `description`: 그룹 설명
                    - `techTags`: 기술 태그
                    - `visibility`: 공개 여부 (PUBLIC | PRIVATE)
                    - `status`: 모집 상태 (OPEN | CLOSED)
                    - `capacity`: 최대 인원
                    - `memberCount`: 현재 인원
                    - `ownerUuid`: 방장 UUID
                    - `createdAt`: 생성 시각
                    - `members`: 가입된 멤버 목록 (각 항목: `userUuid`, `name`, `profileImage`)

                    PUBLIC 그룹은 누구나 조회 가능합니다. PRIVATE 그룹은 멤버/방장/관리자만 조회 가능합니다.
                    """
    )
    public ResponseEntity<ApiResponse<StudyGroupResponse>> detail(@PathVariable("uuid") String uuid) {
        return ResponseEntity.ok(ApiResponse.success(studyGroupService.getDetail(java.util.Objects.requireNonNull(uuid))));
    }

    @GetMapping("/groups")
    @Operation(
            summary = "그룹 검색",
            description = """
                    스터디 그룹을 검색합니다.

                    **Query Parameters:**
                    - `keyword` (선택): 그룹 이름/설명 검색어
                    - `visibility` (선택): 공개 여부 필터 (PUBLIC | PRIVATE) — 인증 시 사용 가능
                    - `status` (선택): 모집 상태 필터 (OPEN | CLOSED)
                    - `page` (선택): 페이지 번호, 0부터 시작 (기본값: 0)
                    - `size` (선택): 페이지 크기 (기본값: 20)

                    **Response:** 페이징된 그룹 목록 (page, size, totalElements, totalPages, content)

                    기본적으로 PUBLIC 그룹만 반환됩니다.
                    """
    )
    public ResponseEntity<ApiResponse<PageResponse<StudyGroupResponse>>> search(@Valid StudyGroupSearchRequest req) {
        return ResponseEntity.ok(ApiResponse.success(studyGroupService.search(java.util.Objects.requireNonNull(req))));
    }

    @GetMapping("/groups/{uuid}/members")
    @Operation(
            summary = "그룹 멤버 목록",
            description = """
                    스터디 그룹의 멤버 목록을 조회합니다.

                    **Path Variable:**
                    - `uuid` (필수): 그룹 UUID

                    **Response:** 멤버 목록 배열 (userUuid, role, joinedAt 등)

                    PUBLIC 그룹은 누구나 조회 가능합니다. PRIVATE 그룹은 멤버/방장/관리자만 조회 가능합니다.
                    """
    )
    public ResponseEntity<ApiResponse<java.util.List<StudyGroupMemberResponse>>> members(@PathVariable("uuid") String uuid) {
        return ResponseEntity.ok(ApiResponse.success(studyGroupService.listMembers(java.util.Objects.requireNonNull(uuid))));
    }

    @PostMapping("/groups/{uuid}/join")
    @Operation(
            summary = "그룹 가입",
            description = """
                    스터디 그룹에 참가 요청을 보냅니다. 로그인이 필요합니다.

                    **Path Variable:**
                    - `uuid` (필수): 가입할 그룹 UUID

                    **Request Body:** 없음

                    **Response:** 성공 메시지 반환 (`"참가요청이 등록되었습니다."`)

                    이미 멤버이거나 대기 중인 요청이 있는 경우 409를 반환합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> join(@PathVariable("uuid") String uuid, Authentication authentication) {
        String userUuid = authentication != null ? authentication.getName() : null;
        if (userUuid == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("UNAUTHORIZED", "인증이 필요합니다."));
        }
        studyGroupService.join(java.util.Objects.requireNonNull(uuid), java.util.Objects.requireNonNull(userUuid)); // 참가요청 생성
        return ResponseEntity.ok(ApiResponse.successMessage("참가요청이 등록되었습니다."));
    }

    @DeleteMapping("/groups/{uuid}/leave")
    @Operation(
            summary = "그룹 탈퇴",
            description = """
                    스터디 그룹에서 탈퇴합니다. 로그인이 필요합니다.

                    **Path Variable:**
                    - `uuid` (필수): 탈퇴할 그룹 UUID

                    **Request Body:** 없음

                    **Response:** 성공 응답 (data: null)

                    방장은 탈퇴할 수 없습니다. 리더 위임 후 탈퇴하세요.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> leave(@PathVariable("uuid") String uuid, Authentication authentication) {
        String userUuid = authentication != null ? authentication.getName() : null;
        if (userUuid == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("UNAUTHORIZED", "인증이 필요합니다."));
        }
        studyGroupService.leave(java.util.Objects.requireNonNull(uuid), java.util.Objects.requireNonNull(userUuid));
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PatchMapping("/groups/{uuid}/status")
    @Operation(
            summary = "그룹 상태 변경(OPEN/CLOSED)",
            description = """
                    그룹의 모집 상태를 변경합니다. 방장/관리자만 가능합니다.

                    **Path Variable:**
                    - `uuid` (필수): 그룹 UUID

                    **Request Body:**
                    - `status` (필수): 변경할 상태 (OPEN | CLOSED)

                    **Response:** 성공 응답 (data: null)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> updateStatus(@PathVariable("uuid") String uuid,
            @Valid @RequestBody StudyGroupStatusUpdateRequest req) {
        studyGroupService.updateStatus(java.util.Objects.requireNonNull(uuid), java.util.Objects.requireNonNull(req));
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PatchMapping("/groups/{uuid}")
    @Operation(
            summary = "리더 위임",
            description = """
                    그룹 방장을 다른 멤버에게 위임합니다. 현재 방장만 가능합니다.

                    **Path Variable:**
                    - `uuid` (필수): 그룹 UUID

                    **Request Body:**
                    - `leaderUserUuid` (필수): 새 방장으로 지정할 사용자 UUID

                    **Response:** 성공 응답 (data: null)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> delegateLeader(@PathVariable("uuid") String uuid,
                                                            @Valid @RequestBody DelegateLeaderRequest req) {
        StudyMemberManageRequest manageReq = new StudyMemberManageRequest();
        manageReq.setUserUuid(req.getLeaderUserUuid());
        studyGroupService.delegateLeader(java.util.Objects.requireNonNull(uuid), java.util.Objects.requireNonNull(manageReq));
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/groups/{uuid}/kick")
    @Operation(
            summary = "멤버 강제탈퇴",
            description = """
                    멤버를 그룹에서 강제 탈퇴시킵니다. 방장/관리자만 가능합니다.

                    **Path Variable:**
                    - `uuid` (필수): 그룹 UUID

                    **Request Body:**
                    - `userUuid` (필수): 강제 탈퇴할 사용자 UUID

                    **Response:** 성공 응답 (data: null)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> kick(@PathVariable("uuid") String uuid,
            @Valid @RequestBody StudyMemberManageRequest req) {
        studyGroupService.kickMember(java.util.Objects.requireNonNull(uuid), java.util.Objects.requireNonNull(req));
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 참가요청 목록(보류중)
    @GetMapping("/groups/{uuid}/requests")
    @Operation(
            summary = "참가요청 목록(보류중)",
            description = """
                    그룹의 대기 중인 참가 요청 목록을 조회합니다. 방장/관리자만 가능합니다.

                    **Path Variable:**
                    - `uuid` (필수): 그룹 UUID

                    **Response:** 참가 요청 목록 배열 (userUuid, requestedAt 등)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<java.util.List<StudyJoinRequestResponse>>> listRequests(@PathVariable("uuid") String uuid) {
        return ResponseEntity.ok(ApiResponse.success(studyGroupService.listPendingRequests(java.util.Objects.requireNonNull(uuid))));
    }

    // 참가요청 승인
    @PostMapping("/groups/{uuid}/requests/{userUuid}/approve")
    @Operation(
            summary = "참가요청 승인",
            description = """
                    특정 사용자의 참가 요청을 승인합니다. 방장/관리자만 가능합니다.

                    **Path Variables:**
                    - `uuid` (필수): 그룹 UUID
                    - `userUuid` (필수): 승인할 사용자 UUID

                    **Request Body:** 없음

                    **Response:** 성공 응답 (data: null)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> approve(@PathVariable("uuid") String uuid, @PathVariable String userUuid) {
        studyGroupService.approveRequest(java.util.Objects.requireNonNull(uuid), java.util.Objects.requireNonNull(userUuid));
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 참가요청 거절
    @PostMapping("/groups/{uuid}/requests/{userUuid}/reject")
    @Operation(
            summary = "참가요청 거절",
            description = """
                    특정 사용자의 참가 요청을 거절합니다. 방장/관리자만 가능합니다.

                    **Path Variables:**
                    - `uuid` (필수): 그룹 UUID
                    - `userUuid` (필수): 거절할 사용자 UUID

                    **Request Body:** 없음

                    **Response:** 성공 응답 (data: null)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> reject(@PathVariable("uuid") String uuid, @PathVariable String userUuid) {
        studyGroupService.rejectRequest(java.util.Objects.requireNonNull(uuid), java.util.Objects.requireNonNull(userUuid));
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 내 참가요청 취소
    @DeleteMapping("/groups/{uuid}/requests/my")
    @Operation(
            summary = "내 참가요청 취소",
            description = """
                    본인이 보낸 참가 요청을 취소합니다. 로그인이 필요합니다.

                    **Path Variable:**
                    - `uuid` (필수): 그룹 UUID

                    **Request Body:** 없음

                    **Response:** 성공 응답 (data: null)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> cancelMy(@PathVariable("uuid") String uuid, Authentication authentication) {
        String userUuid = authentication != null ? authentication.getName() : null;
        if (userUuid == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("UNAUTHORIZED", "인증이 필요합니다."));
        }
        studyGroupService.cancelMyRequest(java.util.Objects.requireNonNull(uuid), java.util.Objects.requireNonNull(userUuid));
        return ResponseEntity.ok(ApiResponse.success());
    }
}
