package com.ada.proj.dto;

import com.ada.proj.enums.GroupVisibility;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class StudyGroupUpdateRequest {

    @Schema(description = "그룹명 (변경 시에만 전달)", example = "스프링 스터디")
    private String name;

    @Schema(description = "그룹 설명 (변경 시에만 전달)")
    private String description;

    @Schema(description = "기술 태그(쉼표구분) (변경 시에만 전달)", example = "spring,java,jpa")
    private String techTags;

    @Schema(description = "공개 여부 (변경 시에만 전달)", example = "PUBLIC")
    private GroupVisibility visibility;

    @Min(1)
    @Max(1000)
    @Schema(description = "최대 인원 (변경 시에만 전달)", example = "10")
    private Integer capacity;

    @Schema(description = "초대 코드/링크 (디스코드, 카카오톡 오픈채팅 등) (변경 시에만 전달)", example = "https://discord.gg/abc1234")
    private String inviteLink;
}
