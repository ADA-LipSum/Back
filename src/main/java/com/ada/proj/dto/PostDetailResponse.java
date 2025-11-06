// src/main/java/com/ada/proj/dto/post/PostDetailResponse.java
package com.ada.proj.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class PostDetailResponse {
    private String postUuid;
    private Long seq;
    private String writerUuid;
    private String title;
    // 에디터 재편집용: Markdown 원문
    private String contentMd;
    private String images;
    private String videos;
    private String writer;
    private LocalDateTime writedAt;
    private LocalDateTime updatedAt;
    private Integer likes;
    private Integer views;
    private Integer comments;
}