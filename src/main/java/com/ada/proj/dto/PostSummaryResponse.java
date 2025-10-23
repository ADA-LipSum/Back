// src/main/java/com/ada/proj/dto/post/PostSummaryResponse.java
package com.ada.proj.dto;

import lombok.*;
import java.time.LocalDateTime;

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
}