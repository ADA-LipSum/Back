package com.ada.proj.dto;

import java.util.List;

import com.ada.proj.enums.CommunityCategory;
import com.ada.proj.enums.PostBoardType;
import com.ada.proj.enums.TechSubTag;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "PostCreateRequest", description = "게시글 생성 요청. 필드가 많지만 각 API마다 필요한 필드만 전달하면 됩니다.")
public class PostCreateRequest {

    @Schema(hidden = true)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String writerUuid;

    @NotBlank
    @Size(max = 20)
    @Schema(description = "제목 (최대 20자) — 모든 게시글 공통 필수", example = "React 상태 관리 질문")
    private String title;

    @JsonAlias({"contentMd"})
    @Schema(description = "본문 — Markdown 또는 HTML. 모든 게시글 공통 선택", example = "zustand와 redux 중 어떤 걸 써야 할까요?")
    private String content;

    @Schema(description = """
            첨부 이미지 URL 목록 (쉼표 구분) — 일반·개발 커뮤니티 공통 선택.
            먼저 POST /api/upload/community/post-media 로 업로드 후 반환된 URL을 넣으세요.""",
            example = "https://bucket.s3.amazonaws.com/community/posts/uuid/img.png")
    private String images;

    @Schema(description = """
            첨부 영상 URL 목록 (쉼표 구분) — 일반·개발 커뮤니티 선택.
            먼저 POST /api/upload/community/post-media 로 업로드 후 반환된 URL을 넣으세요.""",
            example = "https://bucket.s3.amazonaws.com/community/posts/uuid/video.mp4")
    private String videos;

    @Schema(description = "⚠️ 개발 커뮤니티 전용 (POST /api/community/dev/posts). 카테고리 TECH로 자동 설정되므로 생략 가능.",
            example = "TECH")
    private CommunityCategory communityCategory;

    @Schema(description = """
            ⚠️ 개발 커뮤니티 전용 (POST /api/community/dev/posts).
            게시물 유형: QUESTION(질문) · PROJECT(프로젝트) · MEME(밈) · RESOURCE_SHARING(자료 공유) · POLL(투표)""",
            example = "QUESTION")
    private TechSubTag techSubTag;

    @Schema(description = "⚠️ 개발 커뮤니티 전용 (POST /api/community/dev/posts). 언어·기술 태그 배열.",
            example = "[\"React\", \"TypeScript\"]")
    private List<String> techTags;

    @Schema(description = "투표 정보. 개발 커뮤니티 투표 게시글(techSubTag=POLL)에서 사용 가능. 전달 시 techSubTag가 POLL로 자동 설정됨. 투표 선택지·종료 시각 포함.")
    private PollCreateRequest poll;

    @Schema(description = "서버가 자동 설정 (COMMUNITY / BLOG). 보내지 않아도 됩니다.", hidden = true)
    private PostBoardType boardType;

    @Schema(description = "레거시 호환 필드. 신규 요청에서는 사용하지 마세요.", hidden = true)
    private Boolean isDev;

    @Schema(description = "레거시 호환 필드 (techTags 사용 권장). 신규 요청에서는 사용하지 마세요.", hidden = true)
    private String devTags;

    @Schema(description = "블로그 썸네일 URL 전용. 비우면 본문 첫 번째 이미지가 자동 지정됩니다.", hidden = true)
    private String thumbnailImage;

    @Schema(description = """
            게시글 목록 조회 시 첨부 이미지·영상을 노출할지 여부 (선택, 기본값: true).
            `false` 로 설정하면 목록(PostSummaryResponse)의 images/videos/thumbnailImage가 비어서 내려가고, 상세 조회에서만 확인할 수 있습니다.""",
            example = "true")
    private Boolean showMediaInList;
}
