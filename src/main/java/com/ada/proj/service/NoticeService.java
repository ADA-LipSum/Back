package com.ada.proj.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ada.proj.dto.NoticeCreateRequest;
import com.ada.proj.dto.NoticeDetailResponse;
import com.ada.proj.dto.NoticeSummaryResponse;
import com.ada.proj.dto.NoticeUpdateRequest;
import com.ada.proj.dto.PageResponse;
import com.ada.proj.entity.Notice;
import com.ada.proj.entity.User;
import com.ada.proj.enums.NoticeTag;
import com.ada.proj.exception.ForbiddenException;
import com.ada.proj.repository.NoticeRepository;
import com.ada.proj.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService {

    private static final String NOTICE_SEQ_LOCK = "ada_notices_seq";

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final JdbcTemplate jdbcTemplate;

    // ── 목록 조회 ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<NoticeSummaryResponse> list(String tagStr, int page, int size) {
        NoticeTag tag = parseTag(tagStr);
        PageRequest pageable = PageRequest.of(page, size);

        List<Notice> pinned = tag != null
                ? noticeRepository.findPinnedByTag(tag)
                : noticeRepository.findAllByIsPinnedTrueOrderByPinnedOrderAsc();

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

        List<String> urls = splitUrls(notice.getAttachmentUrls());
        List<String> proxyUrls = new ArrayList<>();
        for (int i = 0; i < urls.size(); i++) {
            String key = extractS3Key(urls.get(i));
            String filename = key.substring(key.lastIndexOf('/') + 1);
            proxyUrls.add("/api/notices/" + seq + "/attachments/" + i + "/" + filename);
        }

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
                .attachments(proxyUrls)
                .build();
    }

    // ── 첨부파일 다운로드 ────────────────────────────────────────────

    public record AttachmentData(byte[] bytes, String contentType, String filename) {}

    @Transactional(readOnly = true)
    public AttachmentData downloadAttachment(Long seq, int index) {
        Notice notice = getBySeqOrThrow(seq);
        List<String> urls = splitUrls(notice.getAttachmentUrls());
        if (index < 0 || index >= urls.size()) {
            throw new EntityNotFoundException("첨부파일을 찾을 수 없습니다: index=" + index);
        }
        String s3Url = urls.get(index);
        String key = extractS3Key(s3Url);
        String filename = key.substring(key.lastIndexOf('/') + 1);
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(s3Url))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("첨부파일 다운로드 실패: HTTP " + response.statusCode());
            }
            String ct = response.headers().firstValue("content-type")
                    .map(h -> h.contains(";") ? h.substring(0, h.indexOf(";")).trim() : h)
                    .orElse("application/octet-stream");
            return new AttachmentData(response.body(), ct, filename);
        } catch (EntityNotFoundException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("첨부파일을 가져올 수 없습니다.", e);
        }
    }

    private String extractS3Key(String url) {
        // https://bucket.s3.region.amazonaws.com/key
        int idx = url.indexOf(".amazonaws.com/");
        if (idx == -1) throw new IllegalArgumentException("올바르지 않은 S3 URL: " + url);
        return url.substring(idx + ".amazonaws.com/".length());
    }

    // ── 작성 (ADMIN) ─────────────────────────────────────────────────

    @Transactional
    public Long create(NoticeCreateRequest req, List<MultipartFile> files, Authentication auth) {
        ensureAdmin(auth);

        Long seq = allocateSeq();

        Notice notice = Notice.builder()
                .noticeUuid(UUID.randomUUID().toString())
                .seq(seq)
                .writerUuid(auth.getName())
                .writer(resolveWriter(auth))
                .title(req.getTitle())
                .content(req.getContent())
                .tag(parseTag(req.getTag()))
                .isPinned(false)
                .attachmentUrls(uploadAndJoin(files))
                .build();

        noticeRepository.save(notice);
        return seq;
    }

    // ── 수정 (ADMIN) ─────────────────────────────────────────────────

    @Transactional
    public void update(Long seq, NoticeUpdateRequest req, List<MultipartFile> files, Authentication auth) {
        ensureAdmin(auth);
        Notice notice = getBySeqOrThrow(seq);

        if (req.getTitle() != null) notice.setTitle(req.getTitle());
        if (req.getContent() != null) notice.setContent(req.getContent());
        if (req.getTag() != null) notice.setTag(parseTag(req.getTag()));

        // 새 파일이 전달된 경우 기존 첨부파일 교체
        if (files != null && !files.isEmpty()) {
            notice.setAttachmentUrls(uploadAndJoin(files));
        }
    }

    // ── 삭제 (ADMIN) ─────────────────────────────────────────────────

    @Transactional
    public void delete(Long seq, Authentication auth) {
        ensureAdmin(auth);
        noticeRepository.delete(getBySeqOrThrow(seq));
    }

    // ── 고정 (ADMIN) ─────────────────────────────────────────────────

    @Transactional
    public void pin(Long seq, Authentication auth) {
        ensureAdmin(auth);
        Notice notice = getBySeqOrThrow(seq);
        if (Boolean.TRUE.equals(notice.getIsPinned())) return;

        notice.setIsPinned(true);
        notice.setPinnedOrder(noticeRepository.findMaxPinnedOrder() + 1);
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

    // ── private helpers ──────────────────────────────────────────────

    private String uploadAndJoin(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return null;
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                urls.add(s3Service.uploadNoticeAttachment(file));
            }
        }
        return urls.isEmpty() ? null : String.join(",", urls);
    }

    private List<String> splitUrls(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

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
        User user = userRepository.findByUuid(auth.getName()).orElse(null);
        if (user == null) return null;
        return user.isUseNickname() ? user.getUserNickname() : user.getUserRealname();
    }

    private Long allocateSeq() {
        try {
            Integer locked = jdbcTemplate.queryForObject("SELECT GET_LOCK(?, 10)", Integer.class, NOTICE_SEQ_LOCK);
            if (locked == null || locked != 1) {
                throw new IllegalStateException("Could not acquire notice seq lock");
            }
            Long nextSeq = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(MAX(seq), 0) + 1 FROM notices", Long.class);
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
