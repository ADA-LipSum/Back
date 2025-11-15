package com.ada.proj.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.dto.PurchaseItemRequest;
import com.ada.proj.entity.PurchaseHistory;
import com.ada.proj.entity.StoreItem;
import com.ada.proj.repository.PurchaseHistoryRepository;
import com.ada.proj.repository.StoreItemRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final StoreItemRepository storeItemRepository;
    private final PurchaseHistoryRepository purchaseHistoryRepository;
    private final PointsService pointsService;

    @Transactional
    public PurchaseHistory purchase(PurchaseItemRequest req, Authentication auth) {

        if (auth == null) {
            throw new SecurityException("로그인이 필요합니다.");
        }

        String userUuid = auth.getName();

        StoreItem item = storeItemRepository.findById(req.getItemUuid())
                .orElseThrow(() -> new IllegalArgumentException("아이템을 찾을 수 없습니다."));

        var tx = pointsService.usePoints(
                userUuid,
                item.getPrice(),
                "STORE_PURCHASE",
                null,
                "아이템 구매"
        );

        PurchaseHistory history = PurchaseHistory.builder()
                .userUuid(userUuid)
                .itemUuid(item.getItemUuid())
                .pointsUsed(item.getPrice())
                .pointsTxUuid(tx.getPointsUuid())
                .build();

        return purchaseHistoryRepository.save(history);
    }
}