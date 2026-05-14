package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "CommentUpdateRequest", description = "댓글 수정 요청 바디")
public class CommentUpdateRequest {
    @NotBlank(message = "댓글 내용을 입력해주세요.")
    @Size(max = 2500, message = "댓글은 최대 2500자까지 입력할 수 있습니다.")
    @Schema(description = "수정할 댓글 내용 (최대 2500자)", example = "수정된 댓글 내용")
    private String content;
}
