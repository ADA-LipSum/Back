package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "ProfileLinksRequest", description = "프로필 외부 링크/연동 정보")
public class ProfileLinksRequest {
    @Schema(description = "GitHub 프로필 URL", example = "https://github.com/username")
    private String github;

    @Schema(description = "StackOverflow 프로필 URL", example = "https://stackoverflow.com/users/12345/user")
    private String stackOverflow;

    @Schema(description = "LinkedIn 프로필 URL", example = "https://www.linkedin.com/in/username/")
    private String linkedin;

    // 선택: 연동 여부 (향후 외부 API 연동 시 표시용)
    @Schema(description = "GitHub 연동 여부", example = "true")
    private Boolean githubConnected;

    @Schema(description = "StackOverflow 연동 여부", example = "false")
    private Boolean stackOverflowConnected;

    @Schema(description = "LinkedIn 연동 여부", example = "true")
    private Boolean linkedinConnected;
}
