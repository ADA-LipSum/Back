package com.ada.proj.dto;

import com.ada.proj.enums.GroupVisibility;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StudyGroupCreateRequest {

    @NotBlank
    @Schema(description = "그룹명", example = "스프링 스터디")
    private String name;

    @Schema(description = "그룹 설명")
    private String description;

    @Schema(description = "기술 태그(쉼표구분)", example = "spring,java,jpa")
    private String techTags;

    @NotNull
    @Schema(description = "공개 여부", example = "PUBLIC")
    private GroupVisibility visibility;

    @NotNull
    @Min(1)
    @Max(1000)
    @Schema(description = "최대 인원", example = "10")
    private Integer capacity;

    @Schema(description = "초대 코드/링크 (디스코드, 카카오톡 오픈채팅 등). 승인된 멤버에게만 노출됩니다", example = "https://discord.gg/abc1234")
    private String inviteLink;

    @Schema(description = "활동 시작일", example = "2026-07-01")
    private LocalDate activityStartDate;

    @Schema(description = "활동 종료일", example = "2026-09-30")
    private LocalDate activityEndDate;

    @Schema(description = "활동 방식 (자유 텍스트)", example = "매주 화/목 19~21시 온라인(줌) 진행")
    private String activityType;
}
