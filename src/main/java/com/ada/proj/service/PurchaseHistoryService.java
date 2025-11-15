package com.ada.proj.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Instant;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.ada.proj.dto.PurchaseHistoryResponse;
import com.ada.proj.entity.PurchaseHistory;
import com.ada.proj.entity.Role;
import com.ada.proj.entity.StoreItem;
import com.ada.proj.entity.User;
import com.ada.proj.repository.PurchaseHistoryRepository;
import com.ada.proj.repository.StoreItemRepository;
import com.ada.proj.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PurchaseHistoryService {

    private final PurchaseHistoryRepository purchaseHistoryRepository;
    private final StoreItemRepository storeItemRepository;
    private final UserRepository userRepository;

    private User getRequester(Authentication auth) {
        if (auth == null) throw new SecurityException("로그인이 필요합니다.");
        return userRepository.findByUuid(auth.getName())
                .orElseThrow(() -> new SecurityException("요청자 정보를 찾을 수 없습니다."));
    }

    public List<PurchaseHistoryResponse> filter(
            String userUuid,
            String itemUuid,
            LocalDate startDate,
            LocalDate endDate,
            Authentication auth
    ) {
        User requester = getRequester(auth);

        boolean isAdminOrTeacher = requester.getRole() == Role.ADMIN || requester.getRole() == Role.TEACHER;

        // 학생이면 무조건 본인 uuid만
        if (!isAdminOrTeacher) {
            userUuid = requester.getUuid();
        }

        Instant start = (startDate == null)
                ? null
                : startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

        Instant end = (endDate == null)
                ? null
                : endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

        List<PurchaseHistory> logs = purchaseHistoryRepository.filter(
                userUuid,
                itemUuid,
                start,
                end
        );

        return logs.stream()
                .map(ph -> {
                    StoreItem item = storeItemRepository.findById(ph.getItemUuid()).orElse(null);
                    String itemName = (item == null) ? "(삭제된 아이템)" : item.getName();

                    return new PurchaseHistoryResponse(
                            itemName,
                            ph.getUserUuid(),
                            ph.getCreatedAt()
                    );
                })
                .toList();
    }
}