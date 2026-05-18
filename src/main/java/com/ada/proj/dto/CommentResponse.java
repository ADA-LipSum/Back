package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "CommentResponse", description = "댓글 응답")
public class CommentResponse {

    @Schema(description = "댓글 ID", example = "42")
    private Long commentId;

    @Schema(description = "작성자 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String writerUuid;

    @Schema(description = "작성자 커스텀 ID", example = "hong_gildong")
    private String writerCustomId;

    @Schema(description = "작성자 표시명", example = "홍길동")
    private String writer;

    @Schema(description = "작성자 프로필 이미지 URL")
    private String writerProfileImage;

    @Schema(description = "댓글 내용", example = "좋은 글이네요!")
    private String content;

    @Schema(description = "작성 시각")
    private LocalDateTime createdAt;

    @Schema(description = "첨부 이미지 URL 목록")
    @Builder.Default
    private List<String> images = List.of();

    @Schema(description = "대댓글 목록 (재귀 구조)")
    @Builder.Default
    private List<CommentResponse> children = List.of();
}
