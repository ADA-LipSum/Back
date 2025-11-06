package com.ada.proj.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.ada.proj.entity.Comment;

import lombok.Getter;

@Getter
public class CommentResponse {
    private Long id;
    private String content;
    private String authorName;
    private LocalDateTime createdAt;
    private List<CommentResponse> children;

    public CommentResponse(Comment comment) {
        this.id = comment.getId();
        this.content = comment.getContent();
        // useNickname 값에 따라 표시 이름 결정
        if (comment.getAuthor().isUseNickname()) {
            this.authorName = comment.getAuthor().getUserNickname();
        } else {
            this.authorName = comment.getAuthor().getUserRealname();
        }
        this.createdAt = comment.getCreatedAt();
        this.children = comment.getChildren().stream()
                .map(CommentResponse::new)
                .collect(Collectors.toList());
    }
}
