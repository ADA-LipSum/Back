package com.ada.proj.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "공지사항 수정 요청 (전달한 필드만 업데이트)")
public class NoticeUpdateRequest {

    @Size(max = 100, message = "제목은 100자 이내여야 합니다.")
    @Schema(description = "제목", example = "서비스 점검 안내 (수정)")
    private String title;

    @Schema(description = "본문 (Markdown)")
    private String content;

    @Schema(description = "태그 (EVENT/SERVICE/EMPLOYMENT/OTHER)", example = "SERVICE")
    private String tag;

    @Schema(description = "교체할 첨부파일 ID 목록. null이면 변경 없음, 빈 배열이면 전체 삭제")
    private List<Long> attachmentIds;
}
