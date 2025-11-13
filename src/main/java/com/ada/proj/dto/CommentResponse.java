package com.ada.proj.dto;

import com.ada.proj.entity.Comment;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class CommentResponse {

    private Long id;
    private String content;
    private String authorName;
    private boolean edited;
    private LocalDateTime createdAt;
    private List<CommentResponse> children;

    public CommentResponse(Comment comment) {
        this.id = comment.getId();
        this.content = comment.getContent();
        this.authorName = comment.getAuthorName();
        this.edited = comment.isEdited();
        this.createdAt = comment.getCreatedAt();

        this.children = comment.getChildren().stream()
                .map(CommentResponse::new)
                .toList();
    }
}