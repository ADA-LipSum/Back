package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "UserProjectRequest", description = "프로젝트 등록/수정 요청")
public class UserProjectRequest {

    @Size(max = 100)
    @Schema(description = "프로젝트 제목", example = "Ada 플랫폼 개발")
    private String title;

    @Size(max = 500)
    @Schema(description = "프로젝트 설명", example = "학교 커뮤니티 플랫폼 프로젝트입니다.")
    private String description;

    @Schema(description = "GitHub 저장소 URL", example = "https://github.com/org/repo")
    private String githubUrl;

    @Schema(description = "팀원 모집 여부", example = "true")
    private boolean lookingForTeam;
}
