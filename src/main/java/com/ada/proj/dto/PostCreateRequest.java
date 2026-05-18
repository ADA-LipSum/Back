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
@Schema(name = "PostCreateRequest", description = "게시글 생성 요청")
public class PostCreateRequest {

    @Schema(description = "서버에서 인증 정보로 자동 설정되는 작성자 UUID", accessMode = Schema.AccessMode.READ_ONLY, hidden = true)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String writerUuid;

    @NotBlank
    @Size(max = 20)
    @Schema(description = "게시글 제목. 최대 20자", example = "React 상태 관리 질문")
    private String title;

    @JsonAlias({"contentMd"})
    @Schema(description = "게시글 본문. Markdown 또는 HTML 형식을 사용할 수 있습니다.", example = "zustand와 redux 중 어떤 것을 쓰면 좋을까요?")
    private String content;

    @Schema(description = "첨부 이미지 URL 목록 (쉼표 구분). /api/upload/community/post-media 로 업로드 후 반환된 URL을 전달합니다.", example = "https://bucket.s3.region.amazonaws.com/community/posts/uuid/img.png")
    private String images;

    @Schema(description = "첨부 영상 URL 목록 (쉼표 구분). /api/upload/community/post-media 로 업로드 후 반환된 URL을 전달합니다.", example = "https://bucket.s3.region.amazonaws.com/community/posts/uuid/video.mp4")
    private String videos;

    @Schema(description = "기존 개발글 여부 호환 필드. 신규 API에서는 communityCategory/techSubTag 사용을 권장합니다.", example = "true")
    private Boolean isDev;

    @Schema(description = "기존 기술 태그 CSV 호환 필드. 신규 API에서는 techTags 사용을 권장합니다.", example = "Spring,React")
    private String devTags;

    @Schema(description = "게시판 타입. 전용 API에서는 서버가 COMMUNITY/BLOG/NOTICE로 강제 설정합니다.", example = "COMMUNITY")
    private PostBoardType boardType;

    @Schema(description = "커뮤니티 상위 태그. 커뮤니티 API에서 사용합니다.", example = "TECH")
    private CommunityCategory communityCategory;

    @Schema(description = "기술 하위 태그. communityCategory가 TECH일 때 필수입니다.", example = "QUESTION")
    private TechSubTag techSubTag;

    @Schema(description = "React, MySQL 같은 세부 기술 태그 목록. 다중 선택 가능합니다.", example = "[\"React\", \"MySQL\"]")
    private List<String> techTags;

    @Schema(description = "블로그 썸네일 URL. 비우면 본문 첫 번째 이미지가 자동 지정됩니다.", example = "https://example.com/thumbnail.png")
    private String thumbnailImage;

    @Schema(description = "투표 생성 정보. 커뮤니티 기술 하위 태그가 POLL일 때 사용합니다.")
    private PollCreateRequest poll;
}
