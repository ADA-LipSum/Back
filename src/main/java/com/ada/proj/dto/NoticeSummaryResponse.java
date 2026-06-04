package com.ada.proj.dto;

import java.time.LocalDateTime;

import com.ada.proj.enums.NoticeCategory;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "NoticeSummaryResponse", description = "공지사항 목록 항목 응답")
public class NoticeSummaryResponse {

    @Schema(description = "게시글 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "번호 컬럼 — 프론트에서 isPinned=true 인 경우 📌 아이콘 표시 권장", example = "7")
    private Long seq;

    @Schema(description = "제목", example = "서비스 점검 안내")
    private String title;

    @Schema(description = "작성자 표시명", example = "관리자")
    private String authorName;

    @Schema(description = "작성자 프로필 이미지 URL")
    private String authorProfileImage;

    @Schema(description = "등록일")
    private LocalDateTime createdAt;

    @Schema(description = "조회수", example = "154")
    private Integer views;

    @Schema(description = "분류 (EVENT=행사, SERVICE=서비스, OTHER=기타). 분류 없으면 null", example = "SERVICE")
    private NoticeCategory noticeCategory;

    @Schema(description = "고정 여부 — true 이면 목록 최상단에 pinnedAt 최신순으로 표시", example = "false")
    private Boolean isPinned;

    @Schema(description = "고정 시각. isPinned=false 이면 null")
    private LocalDateTime pinnedAt;
}
