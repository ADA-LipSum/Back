package com.ada.proj.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ada.proj.dto.CommunityEventCreateRequest;
import com.ada.proj.dto.CommunityEventResponse;
import com.ada.proj.dto.CommunityEventUpdateRequest;
import com.ada.proj.entity.CommunityEvent;
import com.ada.proj.repository.CommunityEventRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommunityEventService {

    private static final int MAX_EVENTS = 6;

    private final CommunityEventRepository eventRepository;
    private final S3Service s3Service;

    // ── 활성 이벤트 목록 (공개) ──────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CommunityEventResponse> getActiveEvents() {
        return eventRepository.findActiveEvents(LocalDate.now())
                .stream().map(CommunityEventResponse::from).toList();
    }

    // ── 전체 이벤트 목록 (ADMIN/TEACHER) ────────────────────────────

    @Transactional(readOnly = true)
    public List<CommunityEventResponse> getAllEvents() {
        return eventRepository.findAllByOrderByStartDateAsc()
                .stream().map(CommunityEventResponse::from).toList();
    }

    // ── 생성 (ADMIN/TEACHER) ─────────────────────────────────────────

    @Transactional
    public CommunityEventResponse create(CommunityEventCreateRequest req,
                                         MultipartFile thumbnail,
                                         String creatorUuid) {
        if (eventRepository.count() >= MAX_EVENTS) {
            throw new IllegalStateException("이벤트는 최대 " + MAX_EVENTS + "개까지 등록할 수 있습니다.");
        }
        validateDates(req.getStartDate(), req.getEndDate());

        String thumbnailUrl = uploadThumbnailIfPresent(thumbnail);

        CommunityEvent event = CommunityEvent.builder()
                .title(req.getTitle())
                .thumbnailImage(thumbnailUrl)
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .location(req.getLocation())
                .description(req.getDescription())
                .relatedLink(req.getRelatedLink())
                .createdByUuid(creatorUuid)
                .build();

        return CommunityEventResponse.from(eventRepository.save(event));
    }

    // ── 수정 (ADMIN/TEACHER) ─────────────────────────────────────────

    @Transactional
    public CommunityEventResponse update(Long id,
                                         CommunityEventUpdateRequest req,
                                         MultipartFile thumbnail) {
        CommunityEvent event = getOrThrow(id);

        if (req.getTitle() != null) event.setTitle(req.getTitle());
        if (req.getStartDate() != null) event.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) event.setEndDate(req.getEndDate());
        if (req.getLocation() != null) event.setLocation(req.getLocation());
        if (req.getDescription() != null) event.setDescription(req.getDescription());
        if (req.getRelatedLink() != null) event.setRelatedLink(req.getRelatedLink());

        LocalDate start = event.getStartDate();
        LocalDate end = event.getEndDate();
        validateDates(start, end);

        if (thumbnail != null && !thumbnail.isEmpty()) {
            event.setThumbnailImage(s3Service.uploadEventThumbnail(thumbnail));
        }

        return CommunityEventResponse.from(eventRepository.save(event));
    }

    // ── 활성화/비활성화 (ADMIN/TEACHER) ─────────────────────────────────

    @Transactional
    public CommunityEventResponse setActive(Long id, boolean active) {
        CommunityEvent event = getOrThrow(id);
        event.setActive(active);
        return CommunityEventResponse.from(eventRepository.save(event));
    }

    // ── 삭제 (ADMIN/TEACHER) ─────────────────────────────────────────

    @Transactional
    public void delete(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new EntityNotFoundException("이벤트를 찾을 수 없습니다: " + id);
        }
        eventRepository.deleteById(id);
    }

    // ── private helpers ──────────────────────────────────────────────

    private CommunityEvent getOrThrow(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("이벤트를 찾을 수 없습니다: " + id));
    }

    private String uploadThumbnailIfPresent(MultipartFile thumbnail) {
        if (thumbnail == null || thumbnail.isEmpty()) return null;
        return s3Service.uploadEventThumbnail(thumbnail);
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("종료일은 시작일보다 이전일 수 없습니다.");
        }
    }
}
