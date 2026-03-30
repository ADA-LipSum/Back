package com.ada.proj.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthTokenResponse {

    private String tokenType;
    private String accessToken;
    private String refreshToken;
    private long expiresIn;

    private String uuid;
    private String adminId;
    private String customId;
    private String userRealname;
    private String userNickname;
    private String profileImage;
}
