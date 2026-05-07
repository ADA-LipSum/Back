package com.ada.proj.dto;

import java.time.LocalDateTime;

import com.ada.proj.enums.NotificationType;

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
public class NotificationResponse {

    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private String postUuid;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
