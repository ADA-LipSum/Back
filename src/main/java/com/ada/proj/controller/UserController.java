package com.ada.proj.controller;

import com.ada.proj.dto.*;
import com.ada.proj.enums.Role;
import com.ada.proj.entity.User;
import com.ada.proj.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "회원/프로필", description = "회원 정보, 프로필, 커스텀 로그인 등을 관리하는 API")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ── 목록 / 프로필 조회 ────────────────────────────────────────────────────

    @GetMapping("/users")
    @Operation(
            summary = "유저 목록 조회 (관리자/선생님)",
            description = """
                    전체 사용자 목록을 조회합니다. ADMIN/TEACHER 전용 API입니다.

                    **Query Parameters:**
                    - `role` (선택): 역할 필터 (STUDENT | TEACHER | ADMIN)
                    - `q` (선택): 검색어 — 이름 또는 닉네임에 포함된 문자열로 필터링

                    **Response:** 조건에 맞는 User 목록 배열
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                @ApiResponse(responseCode = "200", description = "조회 성공"),
                @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
                @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content)
            }
    )
    public ResponseEntity<com.ada.proj.dto.ApiResponse<List<User>>> list(
            @Parameter(description = "역할 필터", example = "TEACHER")
            @RequestParam(required = false) Role role,
            @Parameter(description = "검색어(이름/닉네임 포함)", example = "길동")
            @RequestParam(required = false, name = "q") String query
    ) {
        return ResponseEntity.ok(com.ada.proj.dto.ApiResponse.ok(userService.listUsers(role, query)));
    }

    @GetMapping("/users/{uuid}")
    @Operation(
            summary = "유저 정보 조회",
            description = """
                    특정 사용자의 프로필 정보를 조회합니다.

                    **Path Variable:**
                    - `uuid` (필수): 조회할 사용자 UUID
                    """,
            responses = {
                @ApiResponse(responseCode = "200", description = "조회 성공"),
                @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content)
            }
    )
    public ResponseEntity<com.ada.proj.dto.ApiResponse<UserProfileResponse>> get(
            @Parameter(description = "대상 사용자 UUID")
            @PathVariable String uuid) {
        return ResponseEntity.ok(com.ada.proj.dto.ApiResponse.ok(userService.getUserProfile(uuid)));
    }

    @GetMapping("/users/by-username/{username}")
    @Operation(
            summary = "username으로 유저 정보 조회",
            description = """
                    customId(username)으로 사용자 프로필을 조회합니다.
                    프론트엔드 `/profile/{username}` 경로에서 사용합니다.

                    **Path Variable:**
                    - `username` (필수): 사용자의 customId
                    """,
            responses = {
                @ApiResponse(responseCode = "200", description = "조회 성공"),
                @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content)
            }
    )
    public ResponseEntity<com.ada.proj.dto.ApiResponse<UserProfileResponse>> getByUsername(
            @Parameter(description = "사용자 customId (username)")
            @PathVariable String username) {
        return ResponseEntity.ok(com.ada.proj.dto.ApiResponse.ok(userService.getUserProfileByUsername(username)));
    }

    // ── 전체 잔액 조회 ────────────────────────────────────────────────────────

    @GetMapping("/users/balances")
    @Operation(
            summary = "전체 학생 코인/포인트 잔액 목록 (관리자/선생님)",
            description = """
                    모든 STUDENT 역할 사용자의 코인·포인트 잔액을 목록으로 반환합니다.
                    ADMIN/TEACHER 전용입니다.

                    **Response:** uuid, adminId, customId, userRealname, userNickname, coinBalance, pointBalance 목록
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                @ApiResponse(responseCode = "200", description = "조회 성공"),
                @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
                @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content)
            }
    )
    public ResponseEntity<com.ada.proj.dto.ApiResponse<List<UserBalanceSummary>>> getAllStudentBalances(
            Authentication auth) {
        return ResponseEntity.ok(com.ada.proj.dto.ApiResponse.ok(userService.getAllStudentBalances(auth)));
    }

    // ── 프로필 수정 ───────────────────────────────────────────────────────────

    @PatchMapping("/users/{uuid}/role")
    @Operation(
            summary = "권한 변경 (관리자)",
            description = """
                    특정 사용자의 역할을 변경합니다. ADMIN 전용 API입니다.

                    **Path Variable:**
                    - `uuid` (필수): 대상 사용자 UUID

                    **Request Body:**
                    - `role` (필수): 변경할 역할 (STUDENT | TEACHER | ADMIN)
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                @ApiResponse(responseCode = "200", description = "변경 성공"),
                @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content),
                @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content)
            }
    )
    public ResponseEntity<com.ada.proj.dto.ApiResponse<Void>> updateRole(
            @Parameter(description = "대상 사용자 UUID")
            @PathVariable String uuid,
            @Valid @RequestBody UpdateRoleRequest req) {
        userService.updateRole(uuid, req.getRole());
        return ResponseEntity.ok(com.ada.proj.dto.ApiResponse.okMessage("role updated"));
    }

    @PatchMapping("/users/{uuid}/status")
    @Operation(
            summary = "사용자 비활성화/활성화 (관리자/선생님)",
            description = """
                    특정 사용자의 활성 상태를 변경합니다. ADMIN/TEACHER 전용입니다.

                    **Path Variable:**
                    - `uuid` (필수): 대상 사용자 UUID

                    **Request Body:**
                    - `active` (필수): true(활성화) / false(비활성화)

                    **Response:** 성공 메시지 반환
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                @ApiResponse(responseCode = "200", description = "상태 변경 성공"),
                @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
                @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN/TEACHER만 가능)", content = @Content),
                @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content)
            }
    )
    public ResponseEntity<com.ada.proj.dto.ApiResponse<Void>> updateStatus(
            @Parameter(description = "대상 사용자 UUID") @PathVariable String uuid,
            @Valid @RequestBody UserStatusUpdateRequest req,
            Authentication auth) {
        userService.updateStatus(uuid, req.getActive(), auth);
        return ResponseEntity.ok(com.ada.proj.dto.ApiResponse.okMessage("status updated"));
    }

    @DeleteMapping("/users/{uuid}")
    @Operation(
            summary = "사용자 삭제 (관리자/선생님)",
            description = """
                    특정 사용자를 영구 삭제합니다. ADMIN/TEACHER 전용입니다.

                    **Path Variable:**
                    - `uuid` (필수): 삭제할 사용자 UUID

                    **Response:** 성공 메시지 반환
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                @ApiResponse(responseCode = "200", description = "삭제 성공"),
                @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
                @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN/TEACHER만 가능)", content = @Content),
                @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content)
            }
    )
    public ResponseEntity<com.ada.proj.dto.ApiResponse<Void>> deleteUser(
            @Parameter(description = "삭제할 사용자 UUID") @PathVariable String uuid,
            Authentication auth) {
        userService.deleteUser(uuid, auth);
        return ResponseEntity.ok(com.ada.proj.dto.ApiResponse.okMessage("user deleted"));
    }

    @PostMapping("/users/{uuid}/password/reset")
    @Operation(
            summary = "비밀번호 강제 초기화 (관리자/선생님)",
            description = """
                    특정 사용자의 비밀번호를 강제로 초기화합니다. ADMIN/TEACHER 전용입니다.

                    **Path Variable:**
                    - `uuid` (필수): 대상 사용자 UUID

                    **Request Body:**
                    - `newPassword` (필수): 새 비밀번호 (6자 이상)

                    **Response:** 성공 메시지 반환
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                @ApiResponse(responseCode = "200", description = "초기화 성공"),
                @ApiResponse(responseCode = "400", description = "비밀번호 형식 오류", content = @Content),
                @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
                @ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN/TEACHER만 가능)", content = @Content),
                @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content)
            }
    )
    public ResponseEntity<com.ada.proj.dto.ApiResponse<Void>> resetPassword(
            @Parameter(description = "대상 사용자 UUID") @PathVariable String uuid,
            @Valid @RequestBody AdminPasswordResetRequest req,
            Authentication auth) {
        userService.resetPassword(uuid, req.getNewPassword(), auth);
        return ResponseEntity.ok(com.ada.proj.dto.ApiResponse.okMessage("password reset"));
    }

    @PatchMapping("/users/{uuid}/use-nickname")
    @Operation(
            summary = "닉네임으로 이름 표시 여부 토글",
            description = """
                    사용자의 이름 표시 방식을 실명 ↔ 닉네임으로 전환합니다.

                    **Path Variable:**
                    - `uuid` (필수): 대상 사용자 UUID

                    **Response:** 성공 메시지 반환 (`"toggled"`)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<com.ada.proj.dto.ApiResponse<Void>> toggleUseNickname(
            @Parameter(description = "대상 사용자 UUID")
            @PathVariable String uuid) {
        userService.toggleUseNickname(uuid);
        return ResponseEntity.ok(com.ada.proj.dto.ApiResponse.okMessage("toggled"));
    }

    @PatchMapping("/users/{uuid}/profile")
    @Operation(
            summary = "프로필 수정",
            description = """
                    사용자 프로필 정보를 수정합니다. 포함된 필드만 업데이트됩니다.

                    **Path Variable:**
                    - `uuid` (필수): 대상 사용자 UUID

                    **Request Body (모두 선택):**
                    - `nickname`: 닉네임 (최대 10자)
                    - `intro`: 자기소개 (최대 255자)
                    - `techStack`: 기술 스택 목록 (문자열 배열, 예: `["Java", "Spring"]`)
                    - `githubUrl`: GitHub 프로필 URL
                    - `notionUrl`: Notion 페이지 URL
                    - `linkedinUrl`: LinkedIn 프로필 URL
                    - `personalWebsiteUrl`: 개인 웹사이트 URL
                    - `profileBanner`: 프로필 배너 이미지 URL
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<com.ada.proj.dto.ApiResponse<Void>> updateProfile(
            @Parameter(description = "대상 사용자 UUID")
            @PathVariable String uuid,
            @Valid @RequestBody UpdateProfileRequest req) {
        userService.updateProfile(uuid, req);
        return ResponseEntity.ok(com.ada.proj.dto.ApiResponse.okMessage("profile updated"));
    }

    @PatchMapping("/users/{uuid}/custom/password")
    @Operation(
            summary = "커스텀 비밀번호 변경",
            description = """
                    커스텀 로그인 비밀번호를 변경합니다. 본인 또는 ADMIN만 변경 가능합니다.

                    **Path Variable:**
                    - `uuid` (필수): 대상 사용자 UUID

                    **Request Body:**
                    - `currentPassword` (필수): 현재 비밀번호
                    - `newPassword` (필수): 새 비밀번호 (6~255자)

                    현재 비밀번호가 틀린 경우 401을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                @ApiResponse(responseCode = "200", description = "변경 성공"),
                @ApiResponse(responseCode = "401", description = "현재 비밀번호 불일치", content = @Content),
                @ApiResponse(responseCode = "403", description = "본인 또는 관리자만 가능", content = @Content)
            }
    )
    public ResponseEntity<com.ada.proj.dto.ApiResponse<Void>> changePassword(
            @Parameter(description = "대상 사용자 UUID")
            @PathVariable String uuid,
            @Valid @RequestBody UpdatePasswordRequest req,
            Authentication auth) {
        userService.changeCustomPassword(uuid, req, auth);
        return ResponseEntity.ok(com.ada.proj.dto.ApiResponse.okMessage("password updated"));
    }

    @PostMapping("/users/{uuid}/custom")
    @Operation(
            summary = "커스텀 ID/PW 생성 (최초 1회)",
            description = """
                    소셜 로그인 사용자에게 커스텀 ID/PW를 최초 1회 설정합니다. 본인 또는 ADMIN만 사용 가능합니다.

                    **Path Variable:**
                    - `uuid` (필수): 대상 사용자 UUID

                    **Request Body:**
                    - `customId` (필수): 사용할 로그인 ID
                    - `password` (필수): 비밀번호 (6~255자)

                    이미 커스텀 ID가 존재하는 경우 409를 반환합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                @ApiResponse(responseCode = "200", description = "생성 성공"),
                @ApiResponse(responseCode = "409", description = "이미 커스텀 ID 존재", content = @Content)
            }
    )
    public ResponseEntity<com.ada.proj.dto.ApiResponse<Void>> createCustom(
            @Parameter(description = "대상 사용자 UUID")
            @PathVariable String uuid,
            @Valid @RequestBody CreateCustomLoginRequest req,
            Authentication auth) {
        userService.createCustomLogin(uuid, req, auth);
        return ResponseEntity.ok(com.ada.proj.dto.ApiResponse.okMessage("custom login created"));
    }
}
