package com.ada.proj.dto;

import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponse {

    private Long commentId;

    private String writerUuid;
    private String writer;
    private String writerProfileImage;

    private String content;
    private LocalDateTime createdAt;
}