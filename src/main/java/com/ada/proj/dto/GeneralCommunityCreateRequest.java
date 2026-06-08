package com.ada.proj.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

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

    @Schema(description = "첨부 이미지 파일 목록 (jpeg, png, gif, webp, svg / 개당 최대 15MB)")
    private List<MultipartFile> images;

    @Schema(description = "첨부 영상 파일 목록 (mp4 / 개당 최대 100MB)")
    private List<MultipartFile> videos;

    /** PostCreateRequest 로 변환 (images/videos 는 업로드 후 URL을 별도로 설정) */
    public PostCreateRequest toPostCreateRequest() {
        PostCreateRequest req = new PostCreateRequest();
        req.setTitle(title);
        req.setContent(content);
        req.setCommunityCategory(
                communityCategory != null ? communityCategory : CommunityCategory.CHAT
        );
        return req;
    }
}
