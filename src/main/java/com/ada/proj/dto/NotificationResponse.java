package com.ada.proj.dto;

import java.time.LocalDateTime;

import com.ada.proj.enums.NotificationType;

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
@Schema(name = "NotificationResponse", description = "알림 응답")
public class NotificationResponse {

    @Schema(description = "알림 ID", example = "1")
    private Long id;

    @Schema(description = "알림 타입", example = "POLL_ENDED")
    private NotificationType type;

    @Schema(description = "알림 제목", example = "투표 종료")
    private String title;

    @Schema(description = "알림 내용", example = "'다음 프로젝트 DB 선택' 투표가 종료되었습니다.")
    private String message;

    @Schema(description = "관련 게시글 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String postUuid;

    @Schema(description = "읽음 처리 시각. 읽지 않았으면 null")
    private LocalDateTime readAt;

    @Schema(description = "알림 생성 시각")
    private LocalDateTime createdAt;
}
