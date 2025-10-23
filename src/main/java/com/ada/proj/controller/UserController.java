package com.ada.proj.controller;

import com.ada.proj.dto.*;
import com.ada.proj.entity.Role;
import com.ada.proj.entity.User;
import com.ada.proj.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@Tag(name = "회원/프로필")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    @Operation(summary = "유저 목록 조회(관리자)")
    public ResponseEntity<ApiResponse<List<User>>> list(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false, name = "q") String query
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userService.listUsers(role, query)));
    }

    @PatchMapping("/users/{uuid}/role")
    @Operation(summary = "권한 변경(관리자)")
    public ResponseEntity<ApiResponse<Void>> updateRole(@PathVariable String uuid, @Valid @RequestBody UpdateRoleRequest req) {
        userService.updateRole(uuid, req.getRole());
        return ResponseEntity.ok(ApiResponse.okMessage("role updated"));
    }

    @PatchMapping("/users/{uuid}/use-nickname")
    @Operation(summary = "닉네임으로 이름 표시 여부 토글")
    public ResponseEntity<ApiResponse<Void>> toggleUseNickname(@PathVariable String uuid) {
        userService.toggleUseNickname(uuid);
        return ResponseEntity.ok(ApiResponse.okMessage("toggled"));
    }

    @PatchMapping("/users/{uuid}/profile")
    @Operation(summary = "프로필 수정")
    public ResponseEntity<ApiResponse<Void>> updateProfile(@PathVariable String uuid, @Valid @RequestBody UpdateProfileRequest req) {
        userService.updateProfile(uuid, req);
        return ResponseEntity.ok(ApiResponse.okMessage("profile updated"));
    }

    @PatchMapping("/users/{uuid}/custom/password")
    @Operation(summary = "커스텀 비밀번호 변경")
    public ResponseEntity<ApiResponse<Void>> changePassword(@PathVariable String uuid,
                                                            @Valid @RequestBody UpdatePasswordRequest req,
                                                            Authentication auth) {
        userService.changeCustomPassword(uuid, req, auth);
        return ResponseEntity.ok(ApiResponse.okMessage("password updated"));
    }

    @PostMapping("/users/{uuid}/custom")
    @Operation(summary = "커스텀 ID/PW 생성(최초 1회)")
    public ResponseEntity<ApiResponse<Void>> createCustom(@PathVariable String uuid,
                                                          @Valid @RequestBody CreateCustomLoginRequest req,
                                                          Authentication auth) {
        userService.createCustomLogin(uuid, req, auth);
        return ResponseEntity.ok(ApiResponse.okMessage("custom login created"));
    }

    @GetMapping("/users/{uuid}")
    @Operation(summary = "유저 정보 조회")
    public ResponseEntity<ApiResponse<UserProfileResponse>> get(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserProfile(uuid)));
    }
}
