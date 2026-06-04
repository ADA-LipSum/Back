package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "EmojiReactionResponse", description = "이모지 반응 집계 항목")
public class EmojiReactionResponse {

    @Schema(description = "이모지 문자열", example = "👍")
    private String emoji;

    @Schema(description = "해당 이모지의 총 반응 수", example = "5")
    private long count;

    @Schema(description = "현재 로그인 사용자가 이 이모지로 반응했는지 여부 (비로그인 시 항상 false)", example = "true")
    private boolean reacted;
}
