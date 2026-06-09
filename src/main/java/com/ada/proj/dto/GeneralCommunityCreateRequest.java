package com.ada.proj.dto;

import java.util.List;

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
            description = "이미지 URL 배열. 먼저 `POST /api/upload/community/post-media` 로 업로드 후 반환된 URL을 배열로 담아 전달하세요.",
            example = "[\"https://bucket.s3.amazonaws.com/community/posts/uuid/img1.png\"]"
    )
    private List<String> images;

    @Schema(
            description = "영상 URL 배열. 먼저 `POST /api/upload/community/post-media` 로 업로드 후 반환된 URL을 배열로 담아 전달하세요.",
            example = "[\"https://bucket.s3.amazonaws.com/community/posts/uuid/video.mp4\"]"
    )
    private List<String> videos;

    @Schema(
            description = """
                    게시글 목록 조회 시 첨부 이미지·영상을 노출할지 여부 (선택, 기본값: true).
                    `false` 로 설정하면 목록에서는 이미지·영상이 보이지 않고, 상세 조회에서만 확인할 수 있습니다.
                    """,
            example = "true"
    )
    private Boolean showMediaInList;

    @Schema(
            description = """
                    투표 정보 (선택). 전달하면 이 게시글은 투표 게시글이 됩니다.
                    - `question` : 투표 질문
                    - `options` : 선택지 배열 (최소 2개)
                    - `endsAt` : 투표 종료 시각 (ISO 8601)
                    - `anonymous` : 익명 여부
                    """
    )
    private PollCreateRequest poll;

    public PostCreateRequest toPostCreateRequest() {
        PostCreateRequest req = new PostCreateRequest();
        req.setTitle(title);
        req.setContent(content);
        req.setImages(joinOrNull(images));
        req.setVideos(joinOrNull(videos));
        req.setCommunityCategory(communityCategory != null ? communityCategory : CommunityCategory.CHAT);
        req.setPoll(poll);
        req.setShowMediaInList(showMediaInList);
        return req;
    }

    private static String joinOrNull(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return String.join(",", list.stream().filter(s -> s != null && !s.isBlank()).toList());
    }
}
