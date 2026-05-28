package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(name = "CustomStickerSubmitRequest", description = "커스텀 스티커 등록 요청")
public class CustomStickerSubmitRequest {

    @NotBlank
    @Schema(description = "스티커 이름", example = "나만의 고양이 스티커")
    private String name;

    @Schema(description = "스티커 설명 (선택)", example = "귀여운 고양이 캐릭터 스티커입니다.")
    private String description;

    @NotBlank
    @Schema(description = "스티커 이미지 URL", example = "https://example.com/my-sticker.png")
    private String imageUrl;
}
