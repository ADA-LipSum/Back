package com.ada.proj.dto;

import java.util.List;

import com.ada.proj.enums.CommunityCategory;
import com.ada.proj.enums.PostBoardType;
import com.ada.proj.enums.TechSubTag;
import com.fasterxml.jackson.annotation.JsonAlias;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "PostUpdateRequest", description = "게시글 수정 요청. 전달된 필드만 수정됩니다.")
public class PostUpdateRequest {

    @Size(max = 20)
    @Schema(description = "게시글 제목. 최대 20자", example = "React 상태 관리 질문 수정")
    private String title;

    @JsonAlias({"contentMd"})
    @Schema(description = "게시글 본문", example = "수정된 본문입니다.")
    private String content;

    @Schema(description = "이미지 URL CSV")
    private String images;

    @Schema(description = "영상 URL CSV")
    private String videos;

    @Schema(description = "기존 개발글 여부 호환 필드")
    private Boolean isDev;

    @Schema(description = "기존 기술 태그 CSV 호환 필드", example = "Spring,React")
    private String devTags;

    @Schema(description = "게시판 타입. 전용 API에서는 서버가 타입을 강제합니다.", example = "COMMUNITY")
    private PostBoardType boardType;

    @Schema(description = "커뮤니티 상위 태그", example = "TECH")
    private CommunityCategory communityCategory;

    @Schema(description = "기술 하위 태그", example = "TIP")
    private TechSubTag techSubTag;

    @Schema(description = "세부 기술 태그 목록", example = "[\"Spring\", \"JPA\"]")
    private List<String> techTags;

    @Schema(description = "블로그 썸네일 URL", example = "https://example.com/thumbnail.png")
    private String thumbnailImage;

    @Schema(description = "투표 수정 정보. 투표 게시글에서만 사용합니다.")
    private PollCreateRequest poll;
}
