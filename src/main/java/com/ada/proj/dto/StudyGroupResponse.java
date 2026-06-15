package com.ada.proj.dto;

import com.ada.proj.enums.GroupStatus;
import com.ada.proj.enums.GroupVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class StudyGroupResponse {

    @Schema(description = "그룹 UUID")
    private String groupUuid;
    @Schema(description = "썸네일 이미지 URL")
    private String thumbnailImage;
    @Schema(description = "그룹명")
    private String name;
    @Schema(description = "설명")
    private String description;
    @Schema(description = "기술 태그(쉼표구분)")
    private String techTags;
    @Schema(description = "공개 여부")
    private GroupVisibility visibility;
    @Schema(description = "모집 상태")
    private GroupStatus status;
    @Schema(description = "정원")
    private Integer capacity;
    @Schema(description = "방장 UUID")
    private String ownerUuid;
    @Schema(description = "방장 커스텀 ID")
    private String ownerCustomId;
    @Schema(description = "방장 프로필 이미지 URL")
    private String ownerProfileImage;
    @Schema(description = "현재 인원수")
    private Long memberCount;
    @Schema(description = "생성일")
    private Instant createdAt;
    @Schema(description = "수정일")
    private Instant updatedAt;
    @Schema(description = "요청자가 멤버인지 여부")
    private Boolean isMember;
    @Schema(description = "요청자의 그룹 내 역할 (LEADER/MEMBER)")
    private String myRole;
    @Schema(description = "가입된 멤버 목록")
    private List<StudyGroupMemberSummaryResponse> members;
    @Schema(description = "초대 코드/링크 (디스코드, 카카오톡 오픈채팅 등). 승인된 멤버/방장/관리자에게만 노출됩니다")
    private String inviteLink;
    @Schema(description = "활동 시작일")
    private LocalDate activityStartDate;
    @Schema(description = "활동 종료일")
    private LocalDate activityEndDate;
    @Schema(description = "활동 방식 (자유 텍스트)")
    private String activityType;
}
