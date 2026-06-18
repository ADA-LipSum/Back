package com.ada.proj.dto;

import com.ada.proj.enums.GroupStatus;
import com.ada.proj.enums.GroupVisibility;
import com.ada.proj.enums.StudyGroupCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
@Schema(description = "관리자용 스터디 그룹 요약")
public class AdminGroupSummaryResponse {

    @Schema(description = "그룹 UUID")
    private String groupUuid;

    @Schema(description = "그룹 이름")
    private String name;

    @Schema(description = "그룹 설명")
    private String description;

    @Schema(description = "기술 태그")
    private String techTags;

    @Schema(description = "카테고리 (언어공부 | 프로젝트)")
    private StudyGroupCategory category;

    @Schema(description = "공개 여부")
    private GroupVisibility visibility;

    @Schema(description = "그룹 상태")
    private GroupStatus status;

    @Schema(description = "정원")
    private int capacity;

    @Schema(description = "방장 UUID")
    private String ownerUuid;

    @Schema(description = "현재 멤버 수")
    private long memberCount;

    @Schema(description = "생성일시")
    private Instant createdAt;
}
