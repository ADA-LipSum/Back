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
public class PostCreateRequest {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, hidden = true)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String writerUuid;

    @NotBlank
    @Size(max = 20)
    @Schema(example = "게시글 제목")
    private String title;

    @JsonAlias({"contentMd"})
    @Schema(example = "본문 내용입니다.")
    private String content;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, hidden = true)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String images;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, hidden = true)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String videos;

    @Schema(description = "기존 개발글 여부 호환 필드", example = "true")
    private Boolean isDev;

    @Schema(description = "기존 기술 태그 CSV 호환 필드", example = "Spring,React")
    private String devTags;

    @Schema(description = "게시판 타입", example = "COMMUNITY")
    private PostBoardType boardType;

    @Schema(description = "커뮤니티 상위 태그", example = "TECH")
    private CommunityCategory communityCategory;

    @Schema(description = "기술 하위 태그", example = "QUESTION")
    private TechSubTag techSubTag;

    @Schema(description = "기술 세부 태그 목록", example = "[\"React\", \"MySQL\"]")
    private List<String> techTags;

    @Schema(description = "블로그 썸네일 URL. 비우면 본문 첫 번째 이미지가 자동 지정됩니다.")
    private String thumbnailImage;

    @Schema(description = "투표 생성 정보. 커뮤니티 기술 하위 태그가 POLL일 때 사용합니다.")
    private PollCreateRequest poll;
}
