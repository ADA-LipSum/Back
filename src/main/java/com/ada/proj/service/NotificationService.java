package com.ada.proj.service;

import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.dto.NotificationResponse;
import com.ada.proj.dto.PageResponse;
import com.ada.proj.entity.Notification;
import com.ada.proj.enums.NotificationType;
import com.ada.proj.repository.NotificationRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void create(
            @NonNull String recipientUuid,
            @NonNull NotificationType type,
            @NonNull String title,
            @NonNull String message,
            String postUuid
    ) {
        Notification notification = Notification.builder()
                .recipientUuid(recipientUuid)
                .type(type)
                .title(title)
                .message(message)
                .postUuid(postUuid)
                .build();
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(Authentication auth, int page, int size) {
        String userUuid = requireUserUuid(auth);
        Page<NotificationResponse> result = notificationRepository
                .findByRecipientUuidOrderByCreatedAtDesc(
                        userUuid,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toResponse);

        return new PageResponse<>(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getContent()
        );
    }

    @Transactional
    public void markRead(@NonNull Long id, Authentication auth) {
        String userUuid = requireUserUuid(auth);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found: " + id));
        if (!Objects.equals(notification.getRecipientUuid(), userUuid)) {
            throw new AccessDeniedException("본인의 알림만 읽음 처리할 수 있습니다.");
        }
        if (notification.getReadAt() == null) {
            notification.setReadAt(java.time.LocalDateTime.now());
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .postUuid(notification.getPostUuid())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private String requireUserUuid(Authentication auth) {
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }
        return auth.getName();
    }
}
