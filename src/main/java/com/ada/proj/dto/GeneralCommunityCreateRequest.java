package com.ada.proj.dto;

import com.ada.proj.enums.CommunityCategory;
import com.fasterxml.jackson.annotation.JsonAlias;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "GeneralCommunityCreateRequest", description = "일반 커뮤니티 게시글 작성 요청")
public class GeneralCommunityCreateRequest {

    @NotBlank
    @Size(max = 20)
    @Schema(description = "제목 (최대 20자)", example = "점심 같이 먹을 사람~")
    private String title;

    @JsonAlias("contentMd")
    @Schema(description = "본문 — Markdown 또는 HTML", example = "오늘 학식 메뉴 맛있어 보여서요!")
    private String content;

    @Schema(
            description = """
                    게시글 종류 (선택, 기본값: CHAT)
                    - `CHAT` : 잡담
                    - `MEME` : 밈
                    - `PROJECT_SHOWCASE` : 프로젝트 자랑

                    ⚠️ `TECH`(개발 커뮤니티)는 이 엔드포인트에서 사용 불가 → `POST /api/community/dev/posts` 사용
                    """,
            allowableValues = {"CHAT", "MEME", "PROJECT_SHOWCASE"},
            example = "CHAT"
    )
    private CommunityCategory communityCategory;

    @Schema(
            description = "이미지 URL 목록 (쉼표 구분). 먼저 `POST /api/upload/community/post-media` 로 업로드 후 반환된 URL을 넣으세요.",
            example = "https://bucket.s3.ap-northeast-2.amazonaws.com/community/posts/uuid/img.png"
    )
    private String images;

    @Schema(
            description = "영상 URL 목록 (쉼표 구분). 먼저 `POST /api/upload/community/post-media` 로 업로드 후 반환된 URL을 넣으세요.",
            example = "https://bucket.s3.ap-northeast-2.amazonaws.com/community/posts/uuid/video.mp4"
    )
    private String videos;

    /** PostCreateRequest 로 변환 */
    public PostCreateRequest toPostCreateRequest() {
        PostCreateRequest req = new PostCreateRequest();
        req.setTitle(title);
        req.setContent(content);
        req.setImages(images);
        req.setVideos(videos);
        req.setCommunityCategory(
                communityCategory != null ? communityCategory : CommunityCategory.CHAT
        );
        return req;
    }
}
