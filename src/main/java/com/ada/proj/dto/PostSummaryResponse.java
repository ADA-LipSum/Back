// src/main/java/com/ada/proj/dto/PostSummaryResponse.java
package com.ada.proj.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class PostSummaryResponse {

    private String postUuid;
    private Long seq;
    private String title;

    private String writer;              // 작성자명
    private String writerProfileImage;  // 추가됨

    private LocalDateTime writedAt;
    private Integer likes;
    private Integer views;
    private Integer comments;

    private Boolean isDev;
    private String devTags;
    private String tag;
}