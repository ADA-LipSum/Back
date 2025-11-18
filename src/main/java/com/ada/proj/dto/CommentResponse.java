package com.ada.proj.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    // 대댓글 포함 (재귀 구조)
    @Builder.Default
    private List<CommentResponse> children = List.of();
}