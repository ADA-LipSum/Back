package com.ada.proj.dto;

import java.time.LocalDateTime;
import java.util.List;

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
@Schema(description = "공지사항 상세")
public class NoticeDetailResponse {

    @Schema(description = "게시물 순번")
    private Long seq;

    @Schema(description = "태그", example = "SERVICE")
    private NoticeTag tag;

    @Schema(description = "태그 한글명", example = "서비스")
    private String tagLabel;

    @Schema(description = "제목")
    private String title;

    @Schema(description = "작성자")
    private String writer;

    @Schema(description = "등록일")
    private LocalDateTime writedAt;

    @Schema(description = "조회수")
    private Integer views;

    @Schema(description = "본문 내용 (Markdown)")
    private String content;

    @Schema(description = "고정 여부")
    private Boolean isPinned;

    @Schema(description = "첨부파일 URL 목록")
    private List<String> attachments;
}
