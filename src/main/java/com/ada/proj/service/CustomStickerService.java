package com.ada.proj.service;

import com.ada.proj.dto.CustomStickerReviewRequest;
import com.ada.proj.dto.CustomStickerSubmitRequest;
import com.ada.proj.entity.CustomSticker;
import com.ada.proj.entity.User;
import com.ada.proj.entity.UserPoints;
import com.ada.proj.enums.CustomStickerStatus;
import com.ada.proj.repository.CustomStickerRepository;
import com.ada.proj.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class CustomStickerService {

    // 커스텀 스티커 등록 수수료 (포인트)
    public static final int SUBMISSION_FEE = 1500;

    private final CustomStickerRepository customStickerRepository;
    private final UserRepository userRepository;
    private final PointsService pointsService;

    @Transactional
    public CustomSticker submit(String userUuid, CustomStickerSubmitRequest req) {
        UserPoints tx = pointsService.usePoints(
                userUuid,
                SUBMISSION_FEE,
                "custom_sticker",
                null,
                "커스텀 스티커 등록 수수료: " + req.getName()
        );

        CustomSticker sticker = CustomSticker.builder()
                .stickerUuid(UUID.randomUUID().toString())
                .userUuid(userUuid)
                .name(req.getName())
                .description(req.getDescription())
                .imageUrl(req.getImageUrl())
                .status(CustomStickerStatus.PENDING)
                .submissionFee(SUBMISSION_FEE)
                .pointsUuid(tx.getPointsUuid())
                .build();

        return customStickerRepository.save(sticker);
    }

    @Transactional
    public CustomSticker review(String stickerUuid, CustomStickerReviewRequest req, String reviewerUuid) {
        User reviewer = userRepository.findByUuid(reviewerUuid)
                .orElseThrow(() -> new RuntimeException("검수자를 찾을 수 없습니다."));

        String role = reviewer.getRole().name();
        if (!role.equals("ADMIN") && !role.equals("TEACHER")) {
            throw new IllegalStateException("검수는 관리자 및 선생님만 가능합니다.");
        }

        if (req.getStatus() == CustomStickerStatus.PENDING) {
            throw new IllegalArgumentException("검수 결과는 APPROVED 또는 REJECTED여야 합니다.");
        }

        if (req.getStatus() == CustomStickerStatus.REJECTED
                && (req.getRejectionReason() == null || req.getRejectionReason().isBlank())) {
            throw new IllegalArgumentException("반려 시 반려 사유를 입력해야 합니다.");
        }

        CustomSticker sticker = customStickerRepository.findByStickerUuid(stickerUuid)
                .orElseThrow(() -> new EntityNotFoundException("스티커를 찾을 수 없습니다: " + stickerUuid));

        if (sticker.getStatus() != CustomStickerStatus.PENDING) {
            throw new IllegalStateException("이미 검수가 완료된 스티커입니다. 현재 상태: " + sticker.getStatus());
        }

        sticker.setStatus(req.getStatus());
        sticker.setReviewedBy(reviewerUuid);
        sticker.setReviewedAt(Instant.now());

        if (req.getStatus() == CustomStickerStatus.REJECTED) {
            sticker.setRejectionReason(req.getRejectionReason());

            boolean shouldRefund = req.getRefundOnReject() == null || req.getRefundOnReject();
            if (shouldRefund && sticker.getPointsUuid() != null) {
                pointsService.refund(
                        sticker.getUserUuid(),
                        sticker.getPointsUuid(),
                        sticker.getSubmissionFee(),
                        "커스텀 스티커 반려 환불: " + sticker.getName()
                );
            }
        }

        return customStickerRepository.save(sticker);
    }

    @Transactional(readOnly = true)
    public Page<CustomSticker> getMyStickers(String userUuid, CustomStickerStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (status != null) {
            return customStickerRepository.findByUserUuidAndStatusOrderByCreatedAtDesc(userUuid, status, pageable);
        }
        return customStickerRepository.findByUserUuidOrderByCreatedAtDesc(userUuid, pageable);
    }

    @Transactional(readOnly = true)
    public Page<CustomSticker> getPendingStickers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        return customStickerRepository.findByStatusOrderByCreatedAtAsc(CustomStickerStatus.PENDING, pageable);
    }

    @Transactional(readOnly = true)
    public Page<CustomSticker> getAllStickers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return customStickerRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public CustomSticker getSticker(String stickerUuid) {
        return customStickerRepository.findByStickerUuid(stickerUuid)
                .orElseThrow(() -> new EntityNotFoundException("스티커를 찾을 수 없습니다: " + stickerUuid));
    }

    @Transactional(readOnly = true)
    public Page<CustomSticker> getApprovedStickers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return customStickerRepository.findByStatusOrderByCreatedAtAsc(CustomStickerStatus.APPROVED, pageable);
    }
}
