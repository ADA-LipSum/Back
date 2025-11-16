// src/main/java/com/ada/proj/dto/PostDetailResponse.java
package com.ada.proj.dto;

import java.time.LocalDateTime;

import lombok.*;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class PostDetailResponse {

    private String postUuid;
    private Long seq;

    private String writerUuid;
    private String writer;
    private String writerProfileImage;   // 추가됨

    private String title;
    private String content;
    private String images;
    private String videos;

    private LocalDateTime writedAt;
    private LocalDateTime updatedAt;

    private Integer likes;
    private Integer views;
    private Integer comments;

    private Boolean isDev;
    private String devTags;
}