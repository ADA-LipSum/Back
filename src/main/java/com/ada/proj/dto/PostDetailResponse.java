package com.ada.proj.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.ada.proj.enums.CommunityCategory;
import com.ada.proj.enums.PostBoardType;
import com.ada.proj.enums.TechSubTag;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "PostDetailResponse", description = "게시글 상세 응답")
public class PostDetailResponse {

    @Schema(description = "게시글 UUID")
    private String postUuid;

    @Schema(description = "게시글 순번 (목록 페이지의 id와 동일)", example = "1")
    private Long seq;

    @Schema(description = "작성자 UUID")
    private String writerUuid;

    @Schema(description = "작성자 커스텀 ID (프로필 링크용)", example = "gildong99")
    private String writerCustomId;

    @Schema(description = "작성자 표시명 (닉네임 또는 실명)", example = "홍길동")
    private String writer;

    @Schema(description = "작성자 프로필 이미지 URL")
    private String writerProfileImage;

    @Schema(description = "제목", example = "React vs Vue 어떤게 나을까요?")
    private String title;

    @Schema(description = "본문 (Markdown/HTML)")
    private String content;

    @Schema(description = "첨부 이미지 URL 목록")
    private List<String> images;

    @Schema(description = "첨부 영상 URL 목록")
    private List<String> videos;

    @Schema(description = "썸네일 이미지 URL (블로그 전용)")
    private String thumbnailImage;

    @Schema(description = "작성 시각")
    private LocalDateTime writedAt;

    @Schema(description = "최종 수정 시각")
    private LocalDateTime updatedAt;

    @Schema(description = "좋아요 수", example = "12")
    private Integer likes;

    @Schema(description = "조회수", example = "340")
    private Integer views;

    @Schema(description = "댓글 수", example = "5")
    private Integer comments;

    @Schema(description = "개발 게시글 여부 (레거시 필드)", example = "true")
    private Boolean isDev;

    @Schema(description = "기술 태그 CSV (레거시 필드, techTags 사용 권장)", example = "React,TypeScript")
    private String devTags;

    @Schema(description = "기술 태그 목록", example = "[\"React\",\"TypeScript\"]")
    private List<String> techTags;

    @Schema(description = "게시판 타입 (COMMUNITY · BLOG)")
    private PostBoardType boardType;

    @Schema(description = "커뮤니티 상위 카테고리 (CHAT · TECH · MEME · PROJECT_SHOWCASE)")
    private CommunityCategory communityCategory;

    @Schema(description = "개발 커뮤니티 세부 유형 (QUESTION · PROJECT · RESOURCE_SHARING · TIP · POLL · CHAT)")
    private TechSubTag techSubTag;

    @Schema(description = "투표 정보 (techSubTag=POLL 인 경우에만 존재)")
    private PollResponse poll;

    @Schema(description = "현재 로그인 사용자의 좋아요 여부 (비로그인 시 false)", example = "false")
    private Boolean isLiked;

    @Schema(description = "현재 로그인 사용자의 북마크 여부 (비로그인 시 false)", example = "false")
    private Boolean isBookmarked;

    @Schema(description = "이모지 반응 목록 — 이모지별 반응 수 및 내 반응 여부 포함")
    private List<EmojiReactionResponse> emojiReactions;
}
