package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "CommentCreateRequest", description = "댓글 생성 요청 바디")
public class CommentCreateRequest {
    @Schema(hidden = true)
    private Long postSeq;

    @Schema(
        description = "부모 댓글 ID — 대댓글인 경우에만 포함. 일반 댓글이면 이 필드를 보내지 마세요.",
        nullable = true
    )
    private Long parentId;

    @Schema(description = "댓글 내용", example = "재밌는 글이네요!")
    private String content;
}
