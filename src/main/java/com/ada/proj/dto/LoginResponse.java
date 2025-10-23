package com.ada.proj.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String tokenType;
    private String accessToken;
    private long expiresIn;
    private String refreshToken;
}
