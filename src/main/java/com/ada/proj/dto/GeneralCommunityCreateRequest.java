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

    @Schema(description = "첨부 이미지 파일 목록 (jpeg, png, gif, webp, svg / 개당 최대 15MB)")
    private List<MultipartFile> images;

    @Schema(description = "첨부 영상 파일 목록 (mp4 / 개당 최대 100MB)")
    private List<MultipartFile> videos;

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

    /** PostCreateRequest 로 변환 (images/videos 는 업로드 후 URL을 별도로 설정) */
    public PostCreateRequest toPostCreateRequest() {
        PostCreateRequest req = new PostCreateRequest();
        req.setTitle(title);
        req.setContent(content);
        req.setCommunityCategory(CommunityCategory.CHAT);
        req.setPoll(poll);
        req.setShowMediaInList(showMediaInList);
        return req;
    }
}
