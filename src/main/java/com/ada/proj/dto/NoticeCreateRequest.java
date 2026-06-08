package com.ada.proj.dto;

import java.util.List;

import com.ada.proj.enums.NoticeCategory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "NoticeCreateRequest", description = "공지사항 작성 요청")
public class NoticeCreateRequest {

    @NotBlank
    @Size(max = 20)
    @Schema(description = "제목 (최대 20자)", example = "정기 점검 안내")
    private String title;

    @Schema(description = "본문 — Markdown 또는 HTML", example = "안녕하세요. 서버 정기 점검을 안내드립니다.")
    private String content;

    @Schema(description = "공지 분류: EVENT(행사) · SERVICE(서비스) · EMPLOYMENT(취업) · OTHER(기타)", example = "SERVICE")
    private NoticeCategory noticeCategory;

    @Valid
    @Schema(description = "투표 정보 (질문·선택지·종료 시각). 전달 시 해당 공지사항이 투표 게시글이 됨")
    private PollCreateRequest poll;

    @Schema(description = """
            (선택) 미리 POST /api/upload/notice/attachment 로 업로드한 첨부파일 ID 목록.
            attachments 파트로 직접 첨부하는 것과 함께 사용할 수 있으며, 둘 다 공지에 연결됩니다.""",
            example = "[1, 2, 3]")
    private List<Long> attachmentIds;

    public PostCreateRequest toPostCreateRequest() {
        PostCreateRequest request = new PostCreateRequest();
        request.setTitle(title);
        request.setContent(content);
        request.setNoticeCategory(noticeCategory);
        request.setPoll(poll);
        request.setAttachmentIds(attachmentIds);
        return request;
    }
}
