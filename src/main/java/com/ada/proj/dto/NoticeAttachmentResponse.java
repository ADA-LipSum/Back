package com.ada.proj.dto;

import com.ada.proj.entity.NoticeAttachment;
import com.ada.proj.enums.AttachmentType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "공지사항 첨부파일")
public class NoticeAttachmentResponse {

    @Schema(description = "첨부파일 ID")
    private Long id;

    @Schema(description = "원본 파일명", example = "공지문.pdf")
    private String originalFileName;

    @Schema(description = "파일 다운로드 URL")
    private String fileUrl;

    @Schema(description = "파일 크기(bytes)")
    private Long fileSize;

    @Schema(description = "파일 종류", example = "PDF")
    private AttachmentType attachmentType;

    public static NoticeAttachmentResponse from(NoticeAttachment a) {
        return NoticeAttachmentResponse.builder()
                .id(a.getId())
                .originalFileName(a.getOriginalFileName())
                .fileUrl(a.getFileUrl())
                .fileSize(a.getFileSize())
                .attachmentType(a.getAttachmentType())
                .build();
    }
}
