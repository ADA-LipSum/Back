package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "GuestbookRequest", description = "방명록 작성 요청")
public class GuestbookRequest {

    @NotBlank
    @Size(max = 500)
    @Schema(description = "방명록 내용 (최대 500자)", example = "안녕하세요! 좋은 프로필이네요.")
    private String content;
}
