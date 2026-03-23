package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(name = "SocialLinks", description = "소셜 링크 정보")
public class SocialLinks {

    @Schema(description = "GitHub 프로필 URL", example = "https://github.com/username")
    private String githubUrl;

    @Schema(description = "Notion 페이지 URL", example = "https://notion.so/username")
    private String notionUrl;

    @Schema(description = "LinkedIn 프로필 URL", example = "https://linkedin.com/in/username")
    private String linkedinUrl;

    @Schema(description = "개인 웹사이트 URL", example = "https://mysite.dev")
    private String personalWebsiteUrl;
}
