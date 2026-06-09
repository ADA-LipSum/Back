package com.ada.proj.dto;

import java.time.LocalDateTime;

import com.ada.proj.entity.Notice;
import com.ada.proj.enums.NoticeTag;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "공지사항 목록 항목")
public class NoticeSummaryResponse {

    @Schema(description = "게시물 순번 (고정 게시물은 null)", example = "15")
    private Long seq;

    @Schema(description = "고정 여부", example = "false")
    private Boolean isPinned;

    @Schema(description = "태그", example = "SERVICE")
    private NoticeTag tag;

    @Schema(description = "태그 한글명", example = "서비스")
    private String tagLabel;

    @Schema(description = "제목")
    private String title;

    @Schema(description = "등록일")
    private LocalDateTime writedAt;

    @Schema(description = "조회수")
    private Integer views;

    public static NoticeSummaryResponse from(Notice n) {
        return NoticeSummaryResponse.builder()
                .seq(n.getSeq())
                .isPinned(n.getIsPinned())
                .tag(n.getTag())
                .tagLabel(n.getTag() != null ? n.getTag().getLabel() : null)
                .title(n.getTitle())
                .writedAt(n.getWritedAt())
                .views(n.getViews())
                .build();
    }
}
