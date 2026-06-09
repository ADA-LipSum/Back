package com.ada.proj.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "공지사항 작성 요청")
public class NoticeCreateRequest {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이내여야 합니다.")
    @Schema(description = "제목", example = "서비스 점검 안내")
    private String title;

    @Schema(description = "본문 (Markdown)", example = "2026년 6월 10일 새벽 2시부터 ...")
    private String content;

    @Schema(description = "태그 (EVENT/SERVICE/EMPLOYMENT/OTHER)", example = "SERVICE")
    private String tag;

    @Schema(description = "첨부파일 ID 목록 (업로드 후 반환된 ID)", example = "[1, 2]")
    private List<Long> attachmentIds;
}
