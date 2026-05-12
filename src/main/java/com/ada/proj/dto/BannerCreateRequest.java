package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "BannerCreateRequest", description = "커뮤니티 배너 등록/수정 요청")
public class BannerCreateRequest {

    @NotBlank
    @Schema(description = "배너 이미지 URL (권장 크기 1247x320)", example = "https://cdn.example.com/banner.png")
    private String imageUrl;

    @Schema(description = "클릭 시 이동할 URL (선택)", example = "https://example.com/event")
    private String linkUrl;

    @Schema(description = "배너 제목/설명 (선택)", example = "여름 특강 안내")
    private String title;

    @Schema(description = "노출 순서 (낮을수록 앞에 표시, 기본 0)", example = "0")
    private Integer displayOrder;

    @Schema(description = "노출 여부 (기본 true)", example = "true")
    private Boolean active;
}
