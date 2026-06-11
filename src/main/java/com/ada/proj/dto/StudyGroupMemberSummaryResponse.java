package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudyGroupMemberSummaryResponse {

    @Schema(description = "사용자 UUID")
    private String userUuid;
    @Schema(description = "이름")
    private String name;
    @Schema(description = "프로필 사진")
    private String profileImage;
}
