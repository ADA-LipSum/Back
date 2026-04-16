package com.ada.proj.controller;

import com.ada.proj.dto.*;
import com.ada.proj.enums.Role;
import com.ada.proj.entity.User;
import com.ada.proj.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "회원/프로필", description = "회원 정보, 프로필, 커스텀 로그인 등을 관리하는 API")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    @Operation(
            summary = "유저 목록 조회(관리자)",
            description = """
                    전체 사용자 목록을 조회합니다. ADMIN/TEACHER 전용 API입니다.

                    **Query Parameters:**
                    - `role` (선택): 역할 필터 (STUDENT | TEACHER | ADMIN)
                    - `q` (선택): 검색어 — 이름 또는 닉네임에 포함된 문자열로 필터링

                    **Response:** 조건에 맞는 User 목록 배열
                    """
    )
    public ResponseEntity<ApiResponse<List<User>>> list(
            @Parameter(description = "역할 필터", example = "TEACHER")
            @RequestParam(required = false) Role role,
            @Parameter(description = "검색어(이름/닉네임 포함)", example = "길동")
            @RequestParam(required = false, name = "q") String query
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userService.listUsers(role, query)));
    }

    @PatchMapping("/users/{uuid}/role")
    @Operation(
            summary = "권한 변경(관리자)",
            description = """
                    특정 사용자의 역할을 변경합니다. ADMIN 전용 API입니다.

                    **Path Variable:**
                    - `uuid` (필수): 대상 사용자 UUID

                    **Request Body:**
                    - `role` (필수): 변경할 역할 (STUDENT | TEACHER | ADMIN)

                    **Response:** 성공 메시지 반환
                    """
    )
    public ResponseEntity<ApiResponse<Void>> updateRole(
            @Parameter(description = "대상 사용자 UUID")
            @PathVariable String uuid,
            @Valid @RequestBody UpdateRoleRequest req) {
        userService.updateRole(uuid, req.getRole());
        return ResponseEntity.ok(ApiResponse.okMessage("role updated"));
    }

    @PatchMapping("/users/{uuid}/use-nickname")
    @Operation(
            summary = "닉네임으로 이름 표시 여부 토글",
            description = """
                    사용자의 이름 표시 방식을 실명 ↔ 닉네임으로 전환합니다.

                    **Path Variable:**
                    - `uuid` (필수): 대상 사용자 UUID

                    **Request Body:** 없음

                    **Response:** 성공 메시지 반환 (`"toggled"`)
                    """
    )
    public ResponseEntity<ApiResponse<Void>> toggleUseNickname(
            @Parameter(description = "대상 사용자 UUID")
            @PathVariable String uuid) {
        userService.toggleUseNickname(uuid);
        return ResponseEntity.ok(ApiResponse.okMessage("toggled"));
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

                    **Response:** 성공 메시지 반환

                    이미지 파일 업로드는 `POST /api/upload/profile/{uuid}` API를 사용하세요.
                    """
    )
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @Parameter(description = "대상 사용자 UUID")
            @PathVariable String uuid,
            @Valid @RequestBody UpdateProfileRequest req) {
        userService.updateProfile(uuid, req);
        return ResponseEntity.ok(ApiResponse.okMessage("profile updated"));
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

                    **Response:** 성공 메시지 반환

                    현재 비밀번호가 틀린 경우 401을 반환합니다.
                    """
    )
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Parameter(description = "대상 사용자 UUID")
            @PathVariable String uuid,
            @Valid @RequestBody UpdatePasswordRequest req,
            Authentication auth) {
        userService.changeCustomPassword(uuid, req, auth);
        return ResponseEntity.ok(ApiResponse.okMessage("password updated"));
    }

    @PostMapping("/users/{uuid}/custom")
    @Operation(
            summary = "커스텀 ID/PW 생성(최초 1회)",
            description = """
                    소셜 로그인 사용자에게 커스텀 ID/PW를 최초 1회 설정합니다. 본인 또는 ADMIN만 사용 가능합니다.

                    **Path Variable:**
                    - `uuid` (필수): 대상 사용자 UUID

                    **Request Body:**
                    - `customId` (필수): 사용할 로그인 ID
                    - `password` (필수): 비밀번호 (6~255자)

                    **Response:** 성공 메시지 반환

                    이미 커스텀 ID가 존재하는 경우 409를 반환합니다.
                    """
    )
    public ResponseEntity<ApiResponse<Void>> createCustom(
            @Parameter(description = "대상 사용자 UUID")
            @PathVariable String uuid,
            @Valid @RequestBody CreateCustomLoginRequest req,
            Authentication auth) {
        userService.createCustomLogin(uuid, req, auth);
        return ResponseEntity.ok(ApiResponse.okMessage("custom login created"));
    }

    @GetMapping("/users/{uuid}")
    @Operation(
            summary = "유저 정보 조회",
            description = """
                    특정 사용자의 프로필 정보를 조회합니다.

                    **Path Variable:**
                    - `uuid` (필수): 조회할 사용자 UUID

                    **Response:**
                    - `uuid`: 사용자 UUID
                    - `userRealname`: 실명
                    - `userNickname`: 닉네임
                    - `profileImage`: 프로필 이미지 URL
                    - `profileBanner`: 배너 이미지 URL
                    - `intro`: 자기소개
                    - `techStack`: 기술 스택 목록
                    - `role`: 역할
                    - `socialLinks`: 소셜 링크
                    - `activitySummary`: 활동 내역
                    """
    )
    public ResponseEntity<ApiResponse<UserProfileResponse>> get(
            @Parameter(description = "대상 사용자 UUID")
            @PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserProfile(uuid)));
    }

    @GetMapping("/users/by-username/{username}")
    @Operation(
            summary = "username으로 유저 정보 조회",
            description = """
                    customId(username)으로 사용자 프로필을 조회합니다.
                    프론트엔드 `/profile/{username}` 경로에서 사용합니다.

                    **Path Variable:**
                    - `username` (필수): 사용자의 customId
                    """
    )
    public ResponseEntity<ApiResponse<UserProfileResponse>> getByUsername(
            @Parameter(description = "사용자 customId (username)")
            @PathVariable String username) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserProfileByUsername(username)));
    }
}