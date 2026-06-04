package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(name = "EmojiReactionRequest", description = "이모지 반응 토글 요청")
public class EmojiReactionRequest {

    @NotBlank
    @Size(max = 20)
    @Schema(description = "반응할 이모지 문자열. 유니코드 이모지를 그대로 전달하세요.", example = "👍")
    private String emoji;
}
