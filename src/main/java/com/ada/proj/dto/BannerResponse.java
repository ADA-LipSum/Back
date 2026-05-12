package com.ada.proj.dto;

import com.ada.proj.entity.CommunityBanner;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(name = "BannerResponse", description = "커뮤니티 배너 응답")
public class BannerResponse {

    @Schema(description = "배너 ID")
    private Long id;

    @Schema(description = "배너 이미지 URL")
    private String imageUrl;

    @Schema(description = "클릭 시 이동할 URL")
    private String linkUrl;

    @Schema(description = "배너 제목")
    private String title;

    @Schema(description = "노출 순서")
    private Integer displayOrder;

    @Schema(description = "노출 여부")
    private boolean active;

    @Schema(description = "생성 시각")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시각")
    private LocalDateTime updatedAt;

    public static BannerResponse from(CommunityBanner banner) {
        return BannerResponse.builder()
                .id(banner.getId())
                .imageUrl(banner.getImageUrl())
                .linkUrl(banner.getLinkUrl())
                .title(banner.getTitle())
                .displayOrder(banner.getDisplayOrder())
                .active(banner.isActive())
                .createdAt(banner.getCreatedAt())
                .updatedAt(banner.getUpdatedAt())
                .build();
    }
}
