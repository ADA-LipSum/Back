package com.ada.proj.dto;

import java.time.LocalDateTime;

import com.ada.proj.enums.AttachmentType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PostAttachmentResponse", description = "게시글 첨부파일 정보")
public class PostAttachmentResponse {

    @Schema(description = "첨부파일 ID", example = "42")
    private Long id;

    @Schema(description = "원본 파일 이름", example = "2026_학사일정.pdf")
    private String fileName;

    @Schema(description = "S3 다운로드 URL", example = "https://bucket.s3.ap-northeast-2.amazonaws.com/notices/...")
    private String fileUrl;

    @Schema(description = "파일 타입 (IMAGE · VIDEO · HWP · PDF · DOCUMENT · OTHER)", example = "PDF")
    private AttachmentType fileType;

    @Schema(description = "파일 크기 (bytes)", example = "204800")
    private Long fileSize;

    @Schema(description = "업로드 시각")
    private LocalDateTime uploadedAt;
}
