// src/main/java/com/ada/proj/dto/post/PostUpdateRequest.java
package com.ada.proj.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PostUpdateRequest {
    @Size(max = 20)
    private String title;
    // 콘텐츠 원문(마크다운/HTML)
    @JsonAlias({"contentMd"})
    private String content;
    private String images;
    private String videos;
    private String writer;

    // 태그(선택 수정)
    @io.swagger.v3.oas.annotations.media.Schema(description = "개발글 여부")
    private Boolean isDev;
    @io.swagger.v3.oas.annotations.media.Schema(description = "개발 언어 CSV (예: Python,C)")
    private String devTags;
}