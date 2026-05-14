package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "댓글 내용을 입력해주세요.")
    @Size(max = 2500, message = "댓글은 최대 2500자까지 입력할 수 있습니다.")
    @Schema(description = "댓글 내용 (최대 2500자)", example = "재밌는 글이네요!")
    private String content;
}
