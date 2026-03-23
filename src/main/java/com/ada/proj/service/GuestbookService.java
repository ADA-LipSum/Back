package com.ada.proj.service;

import com.ada.proj.dto.GuestbookRequest;
import com.ada.proj.dto.GuestbookResponse;
import com.ada.proj.entity.GuestbookEntry;
import com.ada.proj.entity.User;
import com.ada.proj.exception.ForbiddenException;
import com.ada.proj.exception.UnauthenticatedException;
import com.ada.proj.exception.UserNotFoundException;
import com.ada.proj.repository.GuestbookRepository;
import com.ada.proj.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public Page<GuestbookResponse> listEntries(String targetUuid, Pageable pageable) {
        userRepository.findByUuid(targetUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return guestbookRepository.findByTargetUuidOrderByCreatedAtDesc(targetUuid, pageable)
                .map(this::toResponse);
    }

    public GuestbookResponse addEntry(String targetUuid, GuestbookRequest req, Authentication auth) {
        if (auth == null) {
            throw new UnauthenticatedException("Unauthenticated");
        }
        userRepository.findByUuid(targetUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

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

    public GuestbookResponse updateEntry(String targetUuid, Long entryId, GuestbookRequest req, Authentication auth) {
        if (auth == null) {
            throw new UnauthenticatedException("Unauthenticated");
        }
        GuestbookEntry entry = guestbookRepository.findById(entryId)
                .orElseThrow(() -> new EntityNotFoundException("Entry not found"));
        if (!entry.getTargetUuid().equals(targetUuid)) {
            throw new ForbiddenException("Forbidden");
        }

        String callerUuid = auth.getName();
        boolean isAdmin = isAdmin(auth);
        if (!isAdmin && !entry.getWriterUuid().equals(callerUuid)) {
            throw new ForbiddenException("본인 방명록만 수정할 수 있습니다.");
        }
        entry.setContent(req.getContent());
        return toResponse(entry);
    }

    public void deleteEntry(String targetUuid, Long entryId, Authentication auth) {
        if (auth == null) {
            throw new UnauthenticatedException("Unauthenticated");
        }
        GuestbookEntry entry = guestbookRepository.findById(entryId)
                .orElseThrow(() -> new EntityNotFoundException("Entry not found"));

        String callerUuid = auth.getName();
        boolean isWriter = entry.getWriterUuid().equals(callerUuid);
        boolean isProfileOwner = targetUuid.equals(callerUuid);

        if (!isAdmin(auth) && !isWriter && !isProfileOwner) {
            throw new ForbiddenException("Forbidden");
        }
        guestbookRepository.delete(entry);
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));
    }

    private GuestbookResponse toResponse(GuestbookEntry entry) {
        User writer = userRepository.findByUuid(entry.getWriterUuid()).orElse(null);
        String writerName = null;
        String writerProfileImage = null;
        if (writer != null) {
            writerName = writer.isUseNickname() ? writer.getUserNickname() : writer.getUserRealname();
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
