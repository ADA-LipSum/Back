// src/main/java/com/ada/proj/dto/post/PostUpdateRequest.java
package com.ada.proj.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PostUpdateRequest {
    @Size(max = 20)
    private String title;
    // 단일 콘텐츠 필드: 기본은 Markdown 원문 (HTML 포함 가능)
    private String contentMd;
    private String images;
    private String videos;
    private String writer;
}