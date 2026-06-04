package com.ada.proj.dto;

import java.util.List;

import com.ada.proj.enums.CommunityCategory;
import com.ada.proj.enums.TechSubTag;
import com.fasterxml.jackson.annotation.JsonAlias;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "DevCommunityCreateRequest", description = "개발 커뮤니티 게시글 작성 요청. communityCategory는 자동으로 TECH로 설정됩니다.")
public class DevCommunityCreateRequest {

    @NotBlank
    @Size(max = 20)
    @Schema(description = "제목 (최대 20자)", example = "React 상태 관리 질문")
    private String title;

    @JsonAlias("contentMd")
    @Schema(description = "본문 — Markdown 또는 HTML", example = "zustand와 redux 중 어떤 걸 써야 할까요?")
    private String content;

    @NotNull
    @Schema(
            description = """
                    게시물 유형 (필수)
                    - `QUESTION` : 질문
                    - `PROJECT` : 프로젝트
                    - `RESOURCE_SHARING` : 자료 공유
                    - `POLL` : 투표 (poll 필드 필수)
                    """,
            allowableValues = {"QUESTION", "PROJECT", "RESOURCE_SHARING", "POLL"},
            example = "QUESTION"
    )
    private TechSubTag techSubTag;

    @Schema(
            description = "언어·기술 태그 배열. 인기 태그 통계에 반영됩니다.",
            example = "[\"React\", \"TypeScript\"]"
    )
    private List<String> techTags;

    @Schema(
            description = "이미지 URL 배열. 먼저 `POST /api/upload/community/post-media` 로 업로드 후 반환된 URL을 배열로 담아 전달하세요.",
            example = "[\"https://bucket.s3.amazonaws.com/community/posts/uuid/img.png\"]"
    )
    private List<String> images;

    @Schema(
            description = """
                    투표 정보. `techSubTag=POLL` 일 때만 사용합니다.
                    - `question` : 투표 질문
                    - `options` : 선택지 배열 (최소 2개)
                    - `endsAt` : 투표 종료 시각 (ISO 8601)
                    - `anonymous` : 익명 여부
                    """
    )
    private PollCreateRequest poll;

    /** PostCreateRequest 로 변환 */
    public PostCreateRequest toPostCreateRequest() {
        PostCreateRequest req = new PostCreateRequest();
        req.setTitle(title);
        req.setContent(content);
        req.setImages(joinOrNull(images));
        req.setCommunityCategory(CommunityCategory.TECH);
        req.setTechSubTag(techSubTag);
        req.setTechTags(techTags);
        req.setPoll(poll);
        return req;
    }

    private static String joinOrNull(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return String.join(",", list.stream().filter(s -> s != null && !s.isBlank()).toList());
    }
}
