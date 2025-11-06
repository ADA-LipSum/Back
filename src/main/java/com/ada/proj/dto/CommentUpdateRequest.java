package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "CommentUpdateRequest", description = "댓글 수정 요청 바디")
public class CommentUpdateRequest {
    @Schema(description = "수정할 댓글 내용", example = "수정된 댓글 내용")
    private String content;
}
