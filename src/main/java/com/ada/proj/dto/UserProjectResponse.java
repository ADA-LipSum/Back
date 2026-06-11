package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@Schema(name = "UserProjectResponse", description = "프로젝트 정보")
public class UserProjectResponse {

    @Schema(description = "프로젝트 ID")
    private Long id;

    @Schema(description = "소유자 UUID")
    private String userUuid;

    @Schema(description = "프로젝트 제목")
    private String title;

    @Schema(description = "프로젝트 설명")
    private String description;

    @Schema(description = "GitHub 저장소 URL")
    private String githubUrl;

    @Schema(description = "팀원 모집 여부")
    private boolean lookingForTeam;

    @Schema(description = "우수 프로젝트(순위) 선정 여부")
    private boolean selected;

    @Schema(description = "등록 시각")
    private Instant createdAt;
}
