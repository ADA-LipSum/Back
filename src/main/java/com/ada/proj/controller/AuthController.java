package com.ada.proj.controller;

import com.ada.proj.dto.*;
import com.ada.proj.service.AuthService;
import com.ada.proj.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "로그인/인증")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "관리자 발급 ID/PW 또는 커스텀 ID/PW로 로그인")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse res = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @PostMapping("/admin/login")
    @Operation(summary = "관리자 전용 로그인", description = "ADMIN 역할 계정만 로그인 허용. 일반 계정은 여기서 인증 불가")
    public ResponseEntity<ApiResponse<LoginResponse>> adminLogin(@Valid @RequestBody LoginRequest request) {
        LoginResponse res = authService.adminLogin(request);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @PostMapping("/reissue")
    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 Access/Refresh 재발급")
    public ResponseEntity<ApiResponse<LoginResponse>> reissue(@Valid @RequestBody TokenReissueRequest request) {
        LoginResponse res = authService.reissue(request);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "Refresh Token 폐기")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        String uuid = authentication.getName();
        authService.logout(uuid);
        return ResponseEntity.ok(ApiResponse.okMessage("logged out"));
    }

    @PostMapping("/admin/create")
    @Operation(summary = "관리자: 사용자 생성", description = "관리자가 새로운 사용자 계정을 생성합니다")
    public ResponseEntity<ApiResponse<CreateUserResponse>> createUserByAdmin(@Valid @RequestBody CreateUserRequest req, Authentication authentication) {
        var user = userService.createUserByAdmin(req, authentication);
        CreateUserResponse res = new CreateUserResponse(user.getUuid(), user.getAdminId(), user.getCustomId(), user.getRole());
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @PostMapping("/admin/init")
    @Operation(summary = "초기 관리자 생성", description = "시스템에 ADMIN이 하나도 없을 때 최초의 ADMIN 계정을 생성합니다")
    public ResponseEntity<ApiResponse<CreateUserResponse>> initAdmin(@Valid @RequestBody CreateUserRequest req) {
        var user = userService.createInitialAdmin(req);
        CreateUserResponse res = new CreateUserResponse(user.getUuid(), user.getAdminId(), user.getCustomId(), user.getRole());
        return ResponseEntity.ok(ApiResponse.ok(res));
    }
}
