package com.ada.proj.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.dto.TradeItemCreateRequest;
import com.ada.proj.dto.TradeLogCreateRequest;
import com.ada.proj.dto.TradePurchaseRequest;
import com.ada.proj.entity.TradeItem;
import com.ada.proj.entity.TradeLog;
import com.ada.proj.enums.TradeCategory;
import com.ada.proj.enums.TradeCurrency;
import com.ada.proj.entity.User;
import com.ada.proj.entity.UserCoins;
import com.ada.proj.entity.UserPoints;

import com.ada.proj.entity.UserInventory;
import com.ada.proj.repository.TradeItemRepository;
import com.ada.proj.repository.TradeLogRepository;
import com.ada.proj.repository.UserInventoryRepository;
import com.ada.proj.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.EntityNotFoundException;


@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class TradeService {

    private final TradeItemRepository tradeItemRepository;
    private final TradeLogRepository tradeLogRepository;
    private final UserInventoryRepository userInventoryRepository;
    private final PointsService pointsService;
    private final CoinsService coinsService;
    private final UserRepository userRepository;

    @Transactional
    public TradeItem createItem(TradeItemCreateRequest req, String creatorUuid) {

        if (req.getSubCategory() != null && req.getSubCategory().getParent() != req.getCategory()) {
            throw new IllegalArgumentException(
                    "카테고리가 맞지 않습니다. " + req.getCategory().name() + "의 하위 카테고리가 아닙니다: " + req.getSubCategory().name()
            );
        }

        TradeItem item = TradeItem.builder()
                .itemUuid(UUID.randomUUID().toString())
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .stock(req.getStock() == null ? 0 : req.getStock())
                .active(req.getActive() != null ? req.getActive() : true)
                .category(req.getCategory())
                .subCategory(req.getSubCategory())
                .imageUrl(req.getImageUrl())
                .createdBy(creatorUuid)
                .build();
        return tradeItemRepository.save(item);
    }

    @Transactional
    public void restockItem(String itemUuid, int amount, String operatorUuid) {

        if (amount <= 0) {
            throw new IllegalArgumentException("충전량은 1 이상이어야 합니다.");
        }

        // 재고 충전자인 operator 조회
        User operator = userRepository.findByUuid(operatorUuid)
                .orElseThrow(() -> new RuntimeException("Operator not found"));

        // 권한 검사
        if (!(operator.getRole().name().equals("ADMIN")
                || operator.getRole().name().equals("TEACHER"))) {
            throw new IllegalStateException("재고 충전은 관리자 및 선생님만 가능합니다.");
        }

        TradeItem item = tradeItemRepository.findByItemUuidForUpdate(itemUuid)
                .orElseThrow(() -> new IllegalArgumentException("해당 아이템을 찾을 수 없습니다: " + itemUuid));

        // 재고 증가
        item.setStock(item.getStock() + amount);

        tradeItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public TradeItem getItemDetail(String itemUuid) {
        return tradeItemRepository.findByItemUuid(itemUuid)
                .orElseThrow(() -> new EntityNotFoundException("Item not found: " + itemUuid));
    }

    @Transactional(readOnly = true)
    public Page<TradeItem> searchItems(String keyword, TradeCategory category,
                                       TradeCategory subCategory, Integer minPrice, Integer maxPrice,
                                       Boolean active, int page, int size, String sort, String dir) {

        Specification<TradeItem> spec = (root, query, cb) -> cb.conjunction();


        if (active != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("active"), active));
        }
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.trim() + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(root.get("name"), like),
                    cb.like(root.get("description"), like)
            ));
        }
        if (category != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("category"), category));
        }
        if (category != null && subCategory != null) {
            if (subCategory.getParent() != category) {
                throw new IllegalArgumentException(
                        "카테고리가 맞지 않습니다. " + category.name() + "의 하위 카테고리가 아닙니다: " + subCategory.name()
                );
            }
            spec = spec.and((root, q, cb) -> cb.equal(root.get("subCategory"), subCategory));
        }
        if (minPrice != null) {
            spec = spec.and((root, q, cb) -> cb.ge(root.get("price"), minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and((root, q, cb) -> cb.le(root.get("price"), maxPrice));
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;

        String sortProp = switch (sort == null ? "" : sort) {
            case "price" ->
                "price";
            case "name" ->
                "name";
            case "createdAt", "created_at", "newest" ->
                "createdAt";
            default ->
                "createdAt";
        };

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProp));

        return tradeItemRepository.findAll(spec, pageable);
    }

    @Transactional
    public TradeResult purchase(String userUuid, TradePurchaseRequest req) {

        TradeItem item = tradeItemRepository.findByItemUuidForUpdate(req.getItemUuid())
                .orElseThrow(() -> new EntityNotFoundException("아이템(" + req.getItemUuid() + ")을 찾을 수 없습니다."));

        // 수량 검증
        if (req.getQuantity() <= 0) {
            throw new IllegalArgumentException("구매 수량은 최소 1개여야 합니다.");
        }

        // ETC 1회 구매 제한
            TradeCategory topCategory = item.getCategory().isSubCategory()
                    ? item.getCategory().getParent()
                    : item.getCategory();

            if (topCategory == TradeCategory.ETC) {
            boolean alreadyBought = tradeLogRepository.existsByUserUuidAndItemUuid(userUuid, item.getItemUuid());
            if (alreadyBought) {
                throw new IllegalStateException(
                        "해당 ETC 아이템은 이미 구매하셨습니다. ETC 카테고리 상품은 1회만 구매 가능합니다."
                );
            }
        }

        // 비활성화 상태
        if (item.getActive() == null || !item.getActive()) {
            throw new IllegalStateException("해당 상품은 현재 비활성화되어 구매할 수 없습니다.");
        }

        int qty = req.getQuantity();

        // 재고 확인 (ETC 카테고리는 재고 개념 없이 언제든지 구매 가능)
        if (topCategory != TradeCategory.ETC && item.getStock() < qty) {
            throw new IllegalStateException("재고가 부족합니다. 현재 재고: " + item.getStock());
        }

        int unitPrice = item.getPrice();
        int total = Math.multiplyExact(unitPrice, qty);

        TradeCurrency currency = topCategory == TradeCategory.FOOD ? TradeCurrency.COIN : TradeCurrency.POINT;

        UserPoints pointsTx = null;
        UserCoins coinsTx = null;
        if (currency == TradeCurrency.COIN) {
            coinsTx = coinsService.useCoins(userUuid, total, "물품 구매(코인): " + item.getName());
        } else {
            pointsTx = pointsService.usePoints(
                    userUuid,
                    total,
                    "trade",
                    null,
                    "물품 구매(포인트): " + item.getName()
            );
        }

        // 재고 차감 (ETC 카테고리는 재고 차감 없음)
        if (topCategory != TradeCategory.ETC) {
            item.setStock(item.getStock() - qty);
            tradeItemRepository.save(item);
        }

        // 거래 로그 저장
        TradeLog log = TradeLog.builder()
                .logUuid(UUID.randomUUID().toString())
                .userUuid(userUuid)
                .itemUuid(item.getItemUuid())
                .itemName(item.getName())
                .quantity(qty)
                .unitPrice(unitPrice)
                .totalPoints(total)
                .pointsUuid(pointsTx != null ? pointsTx.getPointsUuid() : null)
                .coinsUuid(coinsTx != null ? coinsTx.getCoinUuid() : null)
                .currency(currency)
                .metadata(null)
                .build();

        tradeLogRepository.save(log);

        // ETC 카테고리 아이템(BANNER, STICKER 등) 구매 시 인벤토리에 저장
        if (topCategory == TradeCategory.ETC) {
            UserInventory inventory = UserInventory.builder()
                    .inventoryUuid(UUID.randomUUID().toString())
                    .userUuid(userUuid)
                    .itemUuid(item.getItemUuid())
                    .build();
            userInventoryRepository.save(inventory);
        }

        return new TradeResult(item, log, currency, pointsTx, coinsTx);
    }

    @Transactional
    public TradeLog createLog(String userUuid, TradeLogCreateRequest req) {
        TradeItem item = tradeItemRepository.findByItemUuid(req.getItemUuid())
                .orElseThrow(() -> new EntityNotFoundException("Item not found: " + req.getItemUuid()));

        String itemName = (req.getItemName() != null && !req.getItemName().isBlank())
                ? req.getItemName()
                : item.getName();

        int qty = Math.max(1, req.getQuantity());
        int unitPrice = item.getPrice();
        int total = req.getTotalPoints() > 0 ? req.getTotalPoints() : Math.multiplyExact(unitPrice, qty);

        TradeLog log = TradeLog.builder()
                .logUuid(UUID.randomUUID().toString())
                .userUuid(userUuid)
                .itemUuid(item.getItemUuid())
                .itemName(itemName)
                .quantity(qty)
                .unitPrice(unitPrice)
                .totalPoints(total)
                .pointsUuid(req.getPointsUuid())
                .currency(TradeCurrency.POINT)
                .metadata(req.getMetadata())
                .build();

        return tradeLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<TradeLog> getMyLogs(String userUuid, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return tradeLogRepository.findByUserUuidOrderByCreatedAtDesc(userUuid, pageable);
    }

    @Transactional
    public void deleteItem(String itemUuid) {
        TradeItem item = tradeItemRepository.findByItemUuid(itemUuid)
                .orElseThrow(()
                        -> new IllegalArgumentException("해당 아이템을 찾을 수 없습니다: " + itemUuid));

        item.setActive(false);
    }

    @lombok.Value
    public static class TradeResult {

        TradeItem item;
        TradeLog log;
        TradeCurrency currency;
        UserPoints pointsTx;
        UserCoins coinsTx;
    }
}
