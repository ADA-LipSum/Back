package com.ada.proj.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.dto.TradeItemCreateRequest;
import com.ada.proj.dto.TradeLogCreateRequest;
import com.ada.proj.dto.TradePurchaseRequest;
import com.ada.proj.dto.TradeItemResponse;
import com.ada.proj.dto.PageResponse;
import com.ada.proj.dto.TradeLogResponse;
import com.ada.proj.entity.TradeItem;
import com.ada.proj.entity.TradeLog;
import com.ada.proj.entity.UserPoints;
import com.ada.proj.repository.TradeItemRepository;
import com.ada.proj.repository.TradeLogRepository;
import com.ada.proj.entity.TradeCategory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class TradeService {

    private final TradeItemRepository tradeItemRepository;
    private final TradeLogRepository tradeLogRepository;
    private final PointsService pointsService;

    @Transactional
    public TradeItem createItem(TradeItemCreateRequest req, String creatorUuid) {
        TradeItem item = TradeItem.builder()
                .itemUuid(UUID.randomUUID().toString())
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .stock(req.getStock())
                .active(req.getActive() != null ? req.getActive() : true)
                .category(req.getCategory())
                .imageUrl(req.getImageUrl())
                .createdBy(creatorUuid)
                .build();
        return tradeItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public TradeItem getItemDetail(String itemUuid) {
        return tradeItemRepository.findByItemUuid(itemUuid)
                .orElseThrow(() -> new EntityNotFoundException("Item not found: " + itemUuid));
    }

    @Transactional(readOnly = true)
    public Page<TradeItem> searchItems(String keyword, TradeCategory category, Integer minPrice, Integer maxPrice,
            Boolean active, int page, int size, String sort, String dir) {

        Specification<TradeItem> spec = Specification.where(null);

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
        if (minPrice != null) {
            spec = spec.and((root, q, cb) -> cb.ge(root.get("price"), minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and((root, q, cb) -> cb.le(root.get("price"), maxPrice));
        }

        Sort.Direction direction = ("asc".equalsIgnoreCase(dir)) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProp = switch (sort == null ? "" : sort) {
            case "price" -> "price";
            case "name" -> "name";
            case "createdAt", "created_at", "newest" -> "createdAt";
            default -> "createdAt";
        };
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProp));
        return tradeItemRepository.findAll(spec, pageable);
    }

    @Transactional
    public TradeResult purchase(String userUuid, TradePurchaseRequest req) {
        // 재고 락
        TradeItem item = tradeItemRepository.findByItemUuidForUpdate(req.getItemUuid())
                .orElseThrow(() -> new EntityNotFoundException("Item not found: " + req.getItemUuid()));
        if (item.getActive() == null || !item.getActive()) {
            throw new IllegalStateException("판매 중이 아닙니다.");
        }
        int qty = Math.max(1, req.getQuantity());
        if (item.getStock() < qty) {
            throw new IllegalStateException("재고가 부족합니다.");
        }
        int unitPrice = item.getPrice();
        int total = Math.multiplyExact(unitPrice, qty);

        // 포인트 사용 처리
        String usedFor = "trade";
        String meta = req.getMetadata();
        UserPoints useTx = pointsService.usePoints(userUuid, total, usedFor, meta, "물품 구매: " + item.getName());

        // 재고 차감 및 저장
        item.setStock(item.getStock() - qty);
        tradeItemRepository.save(item);

        // 거래 로그 기록
        TradeLog log = TradeLog.builder()
                .logUuid(UUID.randomUUID().toString())
                .userUuid(userUuid)
                .itemUuid(item.getItemUuid())
                .itemName(item.getName())
                .quantity(qty)
                .unitPrice(unitPrice)
                .totalPoints(total)
                .pointsUuid(useTx.getPointsUuid())
                .metadata(meta)
                .build();
        tradeLogRepository.save(log);

        return new TradeResult(item, log, useTx);
    }

    @Transactional
    public TradeLog createLog(String userUuid, TradeLogCreateRequest req) {
        TradeItem item = tradeItemRepository.findByItemUuid(req.getItemUuid())
                .orElseThrow(() -> new EntityNotFoundException("Item not found: " + req.getItemUuid()));
        String itemName = (req.getItemName() != null && !req.getItemName().isBlank()) ? req.getItemName() : item.getName();
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
                .metadata(req.getMetadata())
                .build();
        return tradeLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<TradeLog> getMyLogs(String userUuid, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return tradeLogRepository.findByUserUuidOrderByCreatedAtDesc(userUuid, pageable);
    }

    // Helper result type
    @lombok.Value
    public static class TradeResult {
        TradeItem item;
        TradeLog log;
        UserPoints pointsTx;
    }
}
