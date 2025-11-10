package com.ada.proj.dto;

import com.ada.proj.entity.Role;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    // 기존 토큰 관련 필드
    private String tokenType;
    private String accessToken;
    private long expiresIn;
    private String refreshToken;

    // 통합 로그인 응답 추가 필드
    private String uuid;          // 사용자 UUID (주체 식별)
    private Role role;            // 역할 (ADMIN / TEACHER / STUDENT 등)
    private String userRealname;  // 실명
    private String userNickname;  // 닉네임
    private String profileImage;  // 프로필 이미지 URL (첫 로그인 시 기본 identicon 설정 가능)

    @JsonProperty("isFirstLogin")
    private boolean firstLogin;   // 첫 로그인 여부
}
