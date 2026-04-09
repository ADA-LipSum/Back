package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "SocialLinks", description = "Social profile links")
public class SocialLinks {

    @Schema(description = "GitHub profile URL", example = "https://github.com/username")
    private String githubUrl;

    @Schema(description = "Notion page URL", example = "https://notion.so/username")
    private String notionUrl;

    @Schema(description = "LinkedIn profile URL", example = "https://linkedin.com/in/username")
    private String linkedinUrl;

    @Schema(description = "Personal website URL", example = "https://mysite.dev")
    private String personalWebsiteUrl;
}
