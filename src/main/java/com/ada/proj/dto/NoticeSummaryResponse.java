package com.ada.proj.dto;

import java.time.LocalDateTime;

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
@Schema(name = "NoticeSummaryResponse", description = "공지사항 목록 항목 응답")
public class NoticeSummaryResponse {

    @Schema(description = "공지 번호. 게시글 UUID 값입니다.", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @Schema(description = "공지 제목", example = "서비스 점검 안내")
    private String title;

    @Schema(description = "작성자 이름", example = "관리자")
    private String authorName;

    @Schema(description = "작성자 프로필 이미지 URL", example = "https://example.com/profile.png")
    private String authorProfileImage;

    @Schema(description = "등록일")
    private LocalDateTime createdAt;
}
