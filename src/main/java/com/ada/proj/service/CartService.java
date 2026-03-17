package com.ada.proj.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.dto.CartAddRequest;
import com.ada.proj.dto.CartCheckoutResponse;
import com.ada.proj.dto.CartItemResponse;
import com.ada.proj.dto.CartUpdateRequest;
import com.ada.proj.entity.CartItem;
import com.ada.proj.entity.TradeItem;
import com.ada.proj.entity.TradeLog;
import com.ada.proj.entity.UserCoins;
import com.ada.proj.enums.TradeCategory;
import com.ada.proj.enums.TradeCurrency;
import com.ada.proj.repository.CartItemRepository;
import com.ada.proj.repository.TradeItemRepository;
import com.ada.proj.repository.TradeLogRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final TradeItemRepository tradeItemRepository;
    private final TradeLogRepository tradeLogRepository;
    private final CoinsService coinsService;

    public CartItemResponse addToCart(String userUuid, CartAddRequest req) {
        TradeItem item = tradeItemRepository.findByItemUuid(req.getItemUuid())
                .orElseThrow(() -> new EntityNotFoundException("아이템을 찾을 수 없습니다: " + req.getItemUuid()));

        if (item.getActive() == null || !item.getActive()) {
            throw new IllegalStateException("비활성화된 상품은 카트에 담을 수 없습니다.");
        }

        TradeCategory topCategory = item.getCategory().isSubCategory()
                ? item.getCategory().getParent()
                : item.getCategory();

        if (topCategory != TradeCategory.FOOD) {
            throw new IllegalArgumentException("FOOD 카테고리 아이템만 카트에 담을 수 있습니다. ETC 아이템은 직접 구매(POST /api/trade/transactions)를 이용하세요.");
        }

        CartItem cart = cartItemRepository.findByUserUuidAndItemUuid(userUuid, req.getItemUuid())
                .map(existing -> {
                    existing.setQuantity(existing.getQuantity() + req.getQuantity());
                    return cartItemRepository.save(existing);
                })
                .orElseGet(() -> cartItemRepository.save(CartItem.builder()
                        .cartItemUuid(UUID.randomUUID().toString())
                        .userUuid(userUuid)
                        .itemUuid(item.getItemUuid())
                        .quantity(req.getQuantity())
                        .build()));

        return CartItemResponse.of(cart, item);
    }

    @Transactional(readOnly = true)
    public List<CartItemResponse> getCart(String userUuid) {
        List<CartItem> cartItems = cartItemRepository.findByUserUuidOrderByCreatedAtAsc(userUuid);
        List<CartItemResponse> result = new ArrayList<>();
        for (CartItem cart : cartItems) {
            tradeItemRepository.findByItemUuid(cart.getItemUuid())
                    .ifPresent(item -> result.add(CartItemResponse.of(cart, item)));
        }
        return result;
    }

    public CartItemResponse updateQuantity(String userUuid, String cartItemUuid, CartUpdateRequest req) {
        CartItem cart = cartItemRepository.findByCartItemUuid(cartItemUuid)
                .orElseThrow(() -> new EntityNotFoundException("카트 아이템을 찾을 수 없습니다: " + cartItemUuid));

        if (!cart.getUserUuid().equals(userUuid)) {
            throw new SecurityException("본인의 카트 아이템만 수정할 수 있습니다.");
        }

        cart.setQuantity(req.getQuantity());
        cartItemRepository.save(cart);

        TradeItem item = tradeItemRepository.findByItemUuid(cart.getItemUuid())
                .orElseThrow(() -> new EntityNotFoundException("아이템을 찾을 수 없습니다: " + cart.getItemUuid()));

        return CartItemResponse.of(cart, item);
    }

    public void removeFromCart(String userUuid, String cartItemUuid) {
        CartItem cart = cartItemRepository.findByCartItemUuid(cartItemUuid)
                .orElseThrow(() -> new EntityNotFoundException("카트 아이템을 찾을 수 없습니다: " + cartItemUuid));

        if (!cart.getUserUuid().equals(userUuid)) {
            throw new SecurityException("본인의 카트 아이템만 삭제할 수 있습니다.");
        }

        cartItemRepository.delete(cart);
    }

    public CartCheckoutResponse checkout(String userUuid) {
        List<CartItem> cartItems = cartItemRepository.findByUserUuidOrderByCreatedAtAsc(userUuid);

        if (cartItems.isEmpty()) {
            throw new IllegalStateException("카트가 비어있습니다.");
        }

        // 데드락 방지: itemUuid 오름차순 정렬 후 Lock 획득
        cartItems.sort(Comparator.comparing(CartItem::getItemUuid));

        // 1패스: 재고 검증 (Lock 획득, 차감 없이)
        Map<String, TradeItem> itemMap = new HashMap<>();
        for (CartItem cart : cartItems) {
            TradeItem item = tradeItemRepository.findByItemUuidForUpdate(cart.getItemUuid())
                    .orElseThrow(() -> new EntityNotFoundException("아이템을 찾을 수 없습니다: " + cart.getItemUuid()));

            if (item.getActive() == null || !item.getActive()) {
                throw new IllegalStateException("비활성화된 상품이 포함되어 있습니다: " + item.getName());
            }

            if (item.getStock() < cart.getQuantity()) {
                throw new IllegalStateException(
                        "재고가 부족합니다: " + item.getName()
                        + " (요청=" + cart.getQuantity() + ", 재고=" + item.getStock() + ")"
                );
            }

            itemMap.put(item.getItemUuid(), item);
        }

        // 2패스: 재고 차감
        for (CartItem cart : cartItems) {
            TradeItem item = itemMap.get(cart.getItemUuid());
            item.setStock(item.getStock() - cart.getQuantity());
            tradeItemRepository.save(item);
        }

        // 총 코인 계산
        int totalCoins = cartItems.stream()
                .mapToInt(c -> c.getQuantity() * itemMap.get(c.getItemUuid()).getPrice())
                .sum();

        // 코인 일괄 차감 (1회 호출)
        UserCoins coinsTx = coinsService.useCoins(
                userUuid, totalCoins, "카트 일괄 구매 (" + cartItems.size() + "건)"
        );

        // TradeLog 아이템별 생성
        List<TradeLog> logs = new ArrayList<>();
        for (CartItem cart : cartItems) {
            TradeItem item = itemMap.get(cart.getItemUuid());
            logs.add(TradeLog.builder()
                    .logUuid(UUID.randomUUID().toString())
                    .userUuid(userUuid)
                    .itemUuid(item.getItemUuid())
                    .itemName(item.getName())
                    .quantity(cart.getQuantity())
                    .unitPrice(item.getPrice())
                    .totalPoints(item.getPrice() * cart.getQuantity())
                    .coinsUuid(coinsTx.getCoinUuid())
                    .currency(TradeCurrency.COIN)
                    .metadata(null)
                    .build());
        }
        tradeLogRepository.saveAll(logs);

        // 카트 비우기
        cartItemRepository.deleteByUserUuid(userUuid);

        // 응답 조립
        List<CartItemResponse> purchasedItems = new ArrayList<>();
        for (CartItem cart : cartItems) {
            TradeItem item = itemMap.get(cart.getItemUuid());
            purchasedItems.add(CartItemResponse.of(cart, item));
        }

        return CartCheckoutResponse.builder()
                .itemCount(cartItems.size())
                .totalCoins(totalCoins)
                .balanceAfter(coinsTx.getBalanceAfter())
                .items(purchasedItems)
                .build();
    }
}
