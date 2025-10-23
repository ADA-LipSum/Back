package com.ada.proj.controller;

import com.ada.proj.dto.*;
import com.ada.proj.service.AuthService;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "관리자 발급 ID/PW 또는 커스텀 ID/PW로 로그인")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse res = authService.login(request);
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
}
