package com.ada.proj.service;

import com.ada.proj.dto.GuestbookRequest;
import com.ada.proj.dto.GuestbookResponse;
import com.ada.proj.entity.GuestbookEntry;
import com.ada.proj.entity.User;
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

    public GuestbookService(GuestbookRepository guestbookRepository,
                            UserRepository userRepository) {
        this.guestbookRepository = guestbookRepository;
        this.userRepository = userRepository;
    }

    private String resolveTargetUuid(String customId) {
        return userRepository.findByCustomId(customId)
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
        return toResponse(guestbookRepository.save(entry));
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

    public void deleteEntry(String customId, Authentication auth) {
        if (auth == null) {
            throw new UnauthenticatedException("Unauthenticated");
        }
        String targetUuid = resolveTargetUuid(customId);
        String callerUuid = auth.getName();
        GuestbookEntry entry = guestbookRepository.findByTargetUuidAndWriterUuid(targetUuid, callerUuid)
                .orElseThrow(() -> new EntityNotFoundException("Entry not found"));
        guestbookRepository.delete(entry);
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
        String writerProfileImage = null;
        if (writer != null) {
            writerName = writer.isUseNickname() ? writer.getUserNickname() : writer.getCustomId();
            writerProfileImage = writer.getProfileImage();
        }
        return GuestbookResponse.builder()
                .id(entry.getId())
                .writerUuid(entry.getWriterUuid())
                .writerName(writerName)
                .writerProfileImage(writerProfileImage)
                .content(entry.getContent())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }
}
