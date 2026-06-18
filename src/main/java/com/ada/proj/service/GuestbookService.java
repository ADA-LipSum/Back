package com.ada.proj.service;

import com.ada.proj.dto.GuestbookRequest;
import com.ada.proj.dto.GuestbookResponse;
import com.ada.proj.entity.GuestbookEntry;
import com.ada.proj.entity.User;
import com.ada.proj.enums.NotificationType;
import com.ada.proj.enums.RewardActionCode;
import com.ada.proj.exception.UnauthenticatedException;
import com.ada.proj.exception.UserNotFoundException;
import com.ada.proj.repository.GuestbookRepository;
import com.ada.proj.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class GuestbookService {

    private final GuestbookRepository guestbookRepository;
    private final UserRepository userRepository;
    private final RewardService rewardService;
    private final NotificationService notificationService;

    public GuestbookService(GuestbookRepository guestbookRepository,
                            UserRepository userRepository,
                            RewardService rewardService,
                            NotificationService notificationService) {
        this.guestbookRepository = guestbookRepository;
        this.userRepository = userRepository;
        this.rewardService = rewardService;
        this.notificationService = notificationService;
    }

    private String resolveTargetUuid(String customId) {
        return userRepository.findByCustomId(customId)
                .or(() -> userRepository.findByAdminId(customId))
                .orElseThrow(() -> new UserNotFoundException("User not found"))
                .getUuid();
    }

    @Transactional(readOnly = true)
    public List<GuestbookResponse> listEntries(String customId) {
        String targetUuid = resolveTargetUuid(customId);
        List<GuestbookEntry> entries = guestbookRepository.findByTargetUuidOrderByCreatedAtDesc(targetUuid);
        List<String> writerUuids = entries.stream().map(GuestbookEntry::getWriterUuid).distinct().collect(Collectors.toList());
        Map<String, User> userMap = userRepository.findByUuidIn(writerUuids).stream()
                .collect(Collectors.toMap(User::getUuid, u -> u));
        return entries.stream().map(e -> toResponse(e, userMap)).collect(Collectors.toList());
    }

    public GuestbookResponse addEntry(String customId, GuestbookRequest req, Authentication auth) {
        if (auth == null) {
            throw new UnauthenticatedException("Unauthenticated");
        }
        String targetUuid = resolveTargetUuid(customId);
        String writerUuid = auth.getName();
        if (guestbookRepository.existsByTargetUuidAndWriterUuid(targetUuid, writerUuid)) {
            throw new IllegalStateException("이미 방명록을 작성했습니다. 수정 API를 사용하세요.");
        }
        GuestbookEntry entry = GuestbookEntry.builder()
                .targetUuid(targetUuid)
                .writerUuid(writerUuid)
                .content(req.getContent())
                .build();
        GuestbookResponse response = toResponse(guestbookRepository.save(entry));
        rewardService.grant(writerUuid, RewardActionCode.GUESTBOOK_WRITE, targetUuid);
        if (!targetUuid.equals(writerUuid)) {
            notificationService.create(
                    targetUuid,
                    NotificationType.GUESTBOOK_WRITTEN,
                    "방명록",
                    response.getWriterName() + "님이 방명록을 남겼습니다.",
                    null,
                    writerUuid);
        }
        return response;
    }

    public GuestbookResponse updateEntry(String customId, GuestbookRequest req, Authentication auth) {
        if (auth == null) {
            throw new UnauthenticatedException("Unauthenticated");
        }
        String targetUuid = resolveTargetUuid(customId);
        String callerUuid = auth.getName();
        GuestbookEntry entry = guestbookRepository.findByTargetUuidAndWriterUuid(targetUuid, callerUuid)
                .orElseThrow(() -> new EntityNotFoundException("Entry not found"));
        entry.setContent(req.getContent());
        return toResponse(entry);
    }

    private GuestbookResponse toResponse(GuestbookEntry entry) {
        User writer = userRepository.findByUuid(entry.getWriterUuid()).orElse(null);
        return buildResponse(entry, writer);
    }

    private GuestbookResponse toResponse(GuestbookEntry entry, Map<String, User> userMap) {
        User writer = userMap.get(entry.getWriterUuid());
        return buildResponse(entry, writer);
    }

    private GuestbookResponse buildResponse(GuestbookEntry entry, User writer) {
        String writerName = null;
        String writerId = null;
        String writerProfileImage = null;
        if (writer != null) {
            writerName = writer.getUserNickname();
            writerId = writer.getCustomId();
            writerProfileImage = writer.getProfileImage();
        }
        return GuestbookResponse.builder()
                .id(entry.getId())
                .writerUuid(entry.getWriterUuid())
                .writerName(writerName)
                .writerId(writerId)
                .writerProfileImage(writerProfileImage)
                .content(entry.getContent())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }
}
