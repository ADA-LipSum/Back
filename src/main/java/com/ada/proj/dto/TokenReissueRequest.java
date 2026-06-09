package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "TokenReissueRequest", description = "토큰 재발급 요청 바디")
public class TokenReissueRequest {

    @Schema(
            description = "재발급 대상 사용자의 기존 Access Token (필수). 만료된 토큰도 전달할 수 있습니다.",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String accessToken;

    @Schema(
            description = "유효한 Refresh Token (선택). 바디가 비어있으면 쿠키(refreshToken)로 대체됩니다.",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String refreshToken;
}
