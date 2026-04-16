package com.ada.proj.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.dto.UserInventoryResponse;
import com.ada.proj.entity.TradeItem;
import com.ada.proj.entity.UserInventory;
import com.ada.proj.enums.TradeCategory;
import com.ada.proj.exception.ForbiddenException;
import com.ada.proj.repository.TradeItemRepository;
import com.ada.proj.repository.UserInventoryRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final UserInventoryRepository userInventoryRepository;
    private final TradeItemRepository tradeItemRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<UserInventoryResponse> getInventory(String userUuid) {
        return userInventoryRepository.findByUserUuidOrderByAcquiredAtDesc(userUuid).stream()
                .map(inv -> {
                    TradeItem item = tradeItemRepository.findByItemUuid(inv.getItemUuid())
                            .orElse(null);
                    return UserInventoryResponse.builder()
                            .inventoryUuid(inv.getInventoryUuid())
                            .itemUuid(inv.getItemUuid())
                            .itemName(item != null ? item.getName() : null)
                            .imageUrl(item != null ? item.getImageUrl() : null)
                            .category(item != null ? item.getCategory() : null)
                            .subCategory(item != null ? item.getSubCategory() : null)
                            .acquiredAt(inv.getAcquiredAt())
                            .build();
                })
                .toList();
    }

    @Transactional
    public void applyBanner(String userUuid, String inventoryUuid) {
        UserInventory inv = userInventoryRepository.findByInventoryUuid(inventoryUuid)
                .orElseThrow(() -> new EntityNotFoundException("인벤토리 아이템을 찾을 수 없습니다: " + inventoryUuid));

        if (!inv.getUserUuid().equals(userUuid)) {
            throw new ForbiddenException("본인의 인벤토리 아이템만 사용할 수 있습니다.");
        }

        TradeItem item = tradeItemRepository.findByItemUuid(inv.getItemUuid())
                .orElseThrow(() -> new EntityNotFoundException("아이템을 찾을 수 없습니다: " + inv.getItemUuid()));

        TradeCategory subCat = item.getSubCategory();
        if (subCat != TradeCategory.BANNER) {
            throw new IllegalArgumentException("배너 타입의 아이템만 프로필 배너로 설정할 수 있습니다.");
        }

        if (item.getImageUrl() == null || item.getImageUrl().isBlank()) {
            throw new IllegalStateException("해당 배너 아이템에 이미지가 등록되어 있지 않습니다.");
        }

        userService.updateProfileBanner(userUuid, item.getImageUrl());
    }
}
