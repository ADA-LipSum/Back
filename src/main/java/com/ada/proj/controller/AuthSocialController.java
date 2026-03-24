package com.ada.proj.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.AuthMeResponse;
import com.ada.proj.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "로그인/인증")
public class AuthSocialController {

    private final AuthService authService;

    public AuthSocialController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/status")
    @Operation(
            summary = "인증 상태 조회",
            description = """
                    현재 로그인된 사용자의 정보를 반환합니다.

                    **Response:**
                    - `uuid`: 사용자 UUID
                    - `adminId`: 관리자 발급 ID
                    - `customId`: 커스텀 로그인 ID
                    - `role`: 사용자 역할 (STUDENT | TEACHER | ADMIN)
                    - `userRealname`: 실명
                    - `userNickname`: 닉네임
                    - `profileImage`: 프로필 이미지 URL
                    - `isFirstLogin`: 첫 로그인 여부

                    인증되지 않은 요청은 401을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<AuthMeResponse>> status(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(authService.me(authentication)));
    }
}
