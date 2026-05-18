package com.ada.proj.dto;

import com.ada.proj.enums.CommunityCategory;
import com.ada.proj.enums.PostBoardType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Schema(description = "관리자용 게시글 목록 응답")
public class AdminPostSummaryResponse {

    @Schema(description = "게시글 UUID")
    private String postUuid;

    @Schema(description = "작성자 UUID")
    private String writerUuid;

    @Schema(description = "게시글 순번")
    private Long seq;

    @Schema(description = "제목")
    private String title;

    @Schema(description = "작성자 표시명")
    private String writer;

    @Schema(description = "작성 시각")
    private LocalDateTime writedAt;

    @Schema(description = "좋아요 수")
    private Integer likes;

    @Schema(description = "조회수")
    private Integer views;

    @Schema(description = "댓글 수")
    private Integer comments;

    @Schema(description = "게시판 타입")
    private PostBoardType boardType;

    @Schema(description = "커뮤니티 카테고리")
    private CommunityCategory communityCategory;

    @Schema(description = "썸네일 이미지 URL")
    private String thumbnailImage;
}
