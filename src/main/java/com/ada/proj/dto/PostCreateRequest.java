// src/main/java/com/ada/proj/dto/post/PostCreateRequest.java
package com.ada.proj.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PostCreateRequest {
    // 서버 자동 설정
    @Schema(description = "서버에서 자동 설정", accessMode = Schema.AccessMode.READ_ONLY, hidden = true)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String writerUuid;          // 작성자 UUID

    @NotBlank @Size(max = 20)
    private String title;

    // 콘텐츠 원문(마크다운/HTML)
    @JsonAlias({"contentMd"})
    private String content;
    private String images;              // 이미지 URL
    private String videos;              // 영상 URL
    private String writer;              // 닉네임

    // 태그(프론트 분류)
    private Boolean isDev;              // 개발글 여부
    private String devTags;             // 언어 CSV(예: Python,C)
}