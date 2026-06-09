package com.ada.proj.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.dto.NoticeCreateRequest;
import com.ada.proj.dto.NoticeDetailResponse;
import com.ada.proj.dto.NoticeAttachmentResponse;
import com.ada.proj.dto.NoticeSummaryResponse;
import com.ada.proj.dto.NoticeUpdateRequest;
import com.ada.proj.dto.PageResponse;
import com.ada.proj.entity.Notice;
import com.ada.proj.entity.NoticeAttachment;
import com.ada.proj.enums.NoticeTag;
import com.ada.proj.exception.ForbiddenException;
import com.ada.proj.repository.NoticeAttachmentRepository;
import com.ada.proj.repository.NoticeRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService {

    private static final String NOTICE_SEQ_LOCK = "ada_notices_seq";

    private final NoticeRepository noticeRepository;
    private final NoticeAttachmentRepository noticeAttachmentRepository;
    private final JdbcTemplate jdbcTemplate;

    // ── 목록 조회 ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<NoticeSummaryResponse> list(String tagStr, int page, int size) {
        NoticeTag tag = parseTag(tagStr);
        PageRequest pageable = PageRequest.of(page, size);

        // 고정 게시물
        List<Notice> pinned = tag != null
                ? noticeRepository.findPinnedByTag(tag)
                : noticeRepository.findAllByIsPinnedTrueOrderByPinnedOrderAsc();

        // 일반 게시물 (페이징)
        Page<Notice> generalPage = tag != null
                ? noticeRepository.findGeneralByTag(tag, pageable)
                : noticeRepository.findGeneralAll(pageable);

        List<NoticeSummaryResponse> content = new ArrayList<>();
        pinned.forEach(n -> content.add(NoticeSummaryResponse.from(n)));
        generalPage.getContent().forEach(n -> content.add(NoticeSummaryResponse.from(n)));

        return new PageResponse<>(page, size, generalPage.getTotalElements(), generalPage.getTotalPages(), content);
    }

    // ── 상세 조회 ────────────────────────────────────────────────────

    @Transactional
    public NoticeDetailResponse detail(Long seq) {
        Notice notice = getBySeqOrThrow(seq);
        noticeRepository.incrementViews(notice.getNoticeUuid());
        notice.setViews(notice.getViews() + 1);

        List<NoticeAttachmentResponse> attachments = noticeAttachmentRepository
                .findAllByNoticeUuidOrderByIdAsc(notice.getNoticeUuid())
                .stream()
                .map(NoticeAttachmentResponse::from)
                .collect(Collectors.toList());

        return NoticeDetailResponse.builder()
                .seq(notice.getSeq())
                .tag(notice.getTag())
                .tagLabel(notice.getTag() != null ? notice.getTag().getLabel() : null)
                .title(notice.getTitle())
                .writer(notice.getWriter())
                .writedAt(notice.getWritedAt())
                .views(notice.getViews())
                .content(notice.getContent())
                .isPinned(notice.getIsPinned())
                .attachments(attachments)
                .build();
    }

    // ── 작성 (ADMIN) ─────────────────────────────────────────────────

    @Transactional
    public Long create(NoticeCreateRequest req, Authentication auth) {
        ensureAdmin(auth);
        String writerUuid = auth.getName();

        Long seq = allocateSeq();
        String noticeUuid = UUID.randomUUID().toString();

        Notice notice = Notice.builder()
                .noticeUuid(noticeUuid)
                .seq(seq)
                .writerUuid(writerUuid)
                .writer(resolveWriter(auth))
                .title(req.getTitle())
                .content(req.getContent())
                .tag(parseTag(req.getTag()))
                .isPinned(false)
                .build();

        noticeRepository.save(notice);

        linkAttachments(noticeUuid, req.getAttachmentIds());

        return seq;
    }

    // ── 수정 (ADMIN) ─────────────────────────────────────────────────

    @Transactional
    public void update(Long seq, NoticeUpdateRequest req, Authentication auth) {
        ensureAdmin(auth);
        Notice notice = getBySeqOrThrow(seq);

        if (req.getTitle() != null) notice.setTitle(req.getTitle());
        if (req.getContent() != null) notice.setContent(req.getContent());
        if (req.getTag() != null) notice.setTag(parseTag(req.getTag()));

        if (req.getAttachmentIds() != null) {
            noticeAttachmentRepository.deleteAllByNoticeUuid(notice.getNoticeUuid());
            linkAttachments(notice.getNoticeUuid(), req.getAttachmentIds());
        }
    }

    // ── 삭제 (ADMIN) ─────────────────────────────────────────────────

    @Transactional
    public void delete(Long seq, Authentication auth) {
        ensureAdmin(auth);
        Notice notice = getBySeqOrThrow(seq);
        noticeAttachmentRepository.deleteAllByNoticeUuid(notice.getNoticeUuid());
        noticeRepository.delete(notice);
    }

    // ── 고정 (ADMIN) ─────────────────────────────────────────────────

    @Transactional
    public void pin(Long seq, Authentication auth) {
        ensureAdmin(auth);
        Notice notice = getBySeqOrThrow(seq);
        if (Boolean.TRUE.equals(notice.getIsPinned())) return;

        int nextOrder = noticeRepository.findMaxPinnedOrder() + 1;
        notice.setIsPinned(true);
        notice.setPinnedOrder(nextOrder);
        notice.setPinnedAt(LocalDateTime.now());
    }

    @Transactional
    public void unpin(Long seq, Authentication auth) {
        ensureAdmin(auth);
        Notice notice = getBySeqOrThrow(seq);
        if (!Boolean.TRUE.equals(notice.getIsPinned())) return;

        notice.setIsPinned(false);
        notice.setPinnedOrder(null);
        notice.setPinnedAt(null);
    }

    // ── 첨부파일 임시 저장 ────────────────────────────────────────────

    @Transactional
    public NoticeAttachmentResponse saveAttachment(String originalFileName, String fileUrl, long fileSize) {
        NoticeAttachment attachment = NoticeAttachment.builder()
                .noticeUuid(null)  // 아직 게시물에 연결되지 않은 임시 상태
                .originalFileName(originalFileName)
                .fileUrl(fileUrl)
                .fileSize(fileSize)
                .attachmentType(com.ada.proj.enums.AttachmentType.fromFileName(originalFileName))
                .build();
        return NoticeAttachmentResponse.from(noticeAttachmentRepository.save(attachment));
    }

    // ── private helpers ──────────────────────────────────────────────

    private Notice getBySeqOrThrow(Long seq) {
        return noticeRepository.findBySeq(seq)
                .orElseThrow(() -> new EntityNotFoundException("공지사항을 찾을 수 없습니다: " + seq));
    }

    private void ensureAdmin(Authentication auth) {
        if (auth == null) throw new ForbiddenException("인증이 필요합니다.");
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) throw new ForbiddenException("관리자만 접근할 수 있습니다.");
    }

    private NoticeTag parseTag(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value.trim())) return null;
        return NoticeTag.from(value);
    }

    private String resolveWriter(Authentication auth) {
        // UserService 참조 없이 principal 이름을 그대로 사용; 필요 시 UserService 주입 가능
        return auth.getName();
    }

    private void linkAttachments(String noticeUuid, List<Long> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) return;
        List<NoticeAttachment> attachments = noticeAttachmentRepository.findAllById(attachmentIds);
        attachments.forEach(a -> a.setNoticeUuid(noticeUuid));
        noticeAttachmentRepository.saveAll(attachments);
    }

    private Long allocateSeq() {
        try {
            Integer locked = jdbcTemplate.queryForObject("SELECT GET_LOCK(?, 10)", Integer.class, NOTICE_SEQ_LOCK);
            if (locked == null || locked != 1) {
                throw new IllegalStateException("Could not acquire notice seq lock");
            }
            Long nextSeq = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(MAX(seq), 0) + 1 FROM notices",
                    Long.class
            );
            if (nextSeq == null || nextSeq <= 0) {
                throw new IllegalStateException("Could not allocate notice seq");
            }
            return nextSeq;
        } finally {
            try {
                jdbcTemplate.queryForObject("SELECT RELEASE_LOCK(?)", Integer.class, NOTICE_SEQ_LOCK);
            } catch (RuntimeException ex) {
                log.warn("Failed to release notice seq lock: {}", ex.getMessage());
            }
        }
    }
}
