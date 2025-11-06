package com.ada.proj.dto;

import com.ada.proj.entity.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Schema(name = "CommentResponse", description = "댓글 응답 객체")
public class CommentResponse {
    @Schema(description = "댓글 ID (PK)", example = "101")
    private Long id;

    @Schema(description = "댓글 내용", example = "재밌는 글이네요!")
    private String content;

    @Schema(description = "작성자 표시 이름(닉네임/실명 정책에 따라 결정)", example = "허유5412")
    private String authorName;

    @Schema(description = "작성 시간")
    private LocalDateTime createdAt;

    @Schema(description = "대댓글 목록(재귀)")
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
                .toList();
    }
}
