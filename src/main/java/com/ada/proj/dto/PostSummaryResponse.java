// src/main/java/com/ada/proj/dto/post/PostSummaryResponse.java
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
    private String writer;
    private LocalDateTime writedAt;
    private Integer likes;
    private Integer views;
    private Integer comments;
    // 태그
    private Boolean isDev;
    private String devTags;
    private String tag;     // "일반"/"개발(언어)"
}