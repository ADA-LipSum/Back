package com.ada.proj.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.ada.proj.entity.CommunityEvent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "이벤트 위젯 응답")
public class CommunityEventResponse {

    @Schema(description = "이벤트 ID")
    private Long id;

    @Schema(description = "제목")
    private String title;

    @Schema(description = "썸네일 이미지 URL")
    private String thumbnailImage;

    @Schema(description = "시작일", example = "2026-07-01")
    private LocalDate startDate;

    @Schema(description = "종료일", example = "2026-07-03")
    private LocalDate endDate;

    @Schema(description = "장소")
    private String location;

    @Schema(description = "설명")
    private String description;

    @Schema(description = "관련 링크")
    private String relatedLink;

    @Schema(description = "활성화 여부 (false면 위젯에서 숨김)")
    private boolean active;

    @Schema(description = "등록일시")
    private LocalDateTime createdAt;

    public static CommunityEventResponse from(CommunityEvent e) {
        return CommunityEventResponse.builder()
                .id(e.getId())
                .title(e.getTitle())
                .thumbnailImage(e.getThumbnailImage())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .location(e.getLocation())
                .description(e.getDescription())
                .relatedLink(e.getRelatedLink())
                .active(e.isActive())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
