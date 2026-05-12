package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "CommentCreateRequest", description = "댓글 생성 요청 바디")
public class CommentCreateRequest {
    @Schema(description = "게시물 seq (Post.seq)", example = "1")
    private Long postSeq;

    @Schema(description = "상위 댓글 ID(대댓글인 경우만 설정)", example = "123", nullable = true)
    private Long parentId; // nullable

    @Schema(description = "댓글 내용", example = "재밌는 글이네요!")
    private String content;
}
