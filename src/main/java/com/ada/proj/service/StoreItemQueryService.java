package com.ada.proj.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ada.proj.dto.StoreItemResponse;
import com.ada.proj.entity.StoreItem;
import com.ada.proj.entity.StoreType;
import com.ada.proj.repository.StoreItemRepository;
import com.ada.proj.repository.PurchaseHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StoreItemQueryService {

    private final StoreItemRepository storeItemRepository;
    private final PurchaseHistoryRepository purchaseHistoryRepository;

    public StoreItemResponse getItemDetail(String itemUuid) {
        StoreItem item = storeItemRepository.findById(itemUuid)
                .orElseThrow(() -> new IllegalArgumentException("해당 아이템을 찾을 수 없습니다."));
        return StoreItemResponse.from(item);
    }

    public List<StoreItemResponse> searchItems(
            String name,
            Integer minPrice,
            Integer maxPrice,
            String category,
            StoreType storeType,
            String sort
    ) {

        List<StoreItem> list = storeItemRepository.filter(
                (name == null || name.isBlank()) ? null : name,
                minPrice,
                maxPrice,
                (category == null || category.isBlank()) ? null : category,
                storeType
        );

        // 아이템별 판매량 집계
        Map<String, Long> soldCountMap = purchaseHistoryRepository.countPurchasesGroupByItem()
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],      // itemUuid
                        row -> (Long) row[1]         // count
                ));

        // 정렬
        if (sort == null || sort.isBlank() || sort.equals("name")) {
            list.sort(Comparator.comparing(StoreItem::getName));
        } else {
            switch (sort) {
                case "price_asc" ->
                        list.sort(Comparator.comparingInt(StoreItem::getPrice));

                case "price_desc" ->
                        list.sort(Comparator.comparingInt(StoreItem::getPrice).reversed());

                case "sold_desc" ->
                        list.sort((a, b) ->
                                Long.compare(
                                        soldCountMap.getOrDefault(b.getItemUuid(), 0L),
                                        soldCountMap.getOrDefault(a.getItemUuid(), 0L)
                                )
                        );

                default ->
                        list.sort(Comparator.comparing(StoreItem::getName));
            }
        }

        return list.stream()
                .map(StoreItemResponse::from)
                .toList();
    }
}