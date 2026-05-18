package com.ada.proj.dto;

import com.ada.proj.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "관리자 공지 알림 발송 요청")
public class AdminNotificationSendRequest {

    @NotBlank
    @Schema(description = "알림 제목", example = "시스템 점검 안내")
    private String title;

    @NotBlank
    @Schema(description = "알림 내용", example = "오늘 밤 12시에 시스템 점검이 예정되어 있습니다.")
    private String message;

    @Schema(description = "대상 역할 (null이면 전체 발송)", example = "STUDENT")
    private Role targetRole;

    @Schema(description = "관련 게시글 UUID (선택)", example = "post-uuid-1234")
    private String postUuid;
}
