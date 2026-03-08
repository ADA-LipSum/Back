package com.ada.proj.service;

import com.ada.proj.dto.TradePurchaseRequest;
import com.ada.proj.entity.TradeItem;
import com.ada.proj.entity.TradeLog;
import com.ada.proj.entity.UserCoins;
import com.ada.proj.entity.UserPoints;
import com.ada.proj.enums.PointChangeType;
import com.ada.proj.enums.TradeCategory;
import com.ada.proj.repository.TradeItemRepository;
import com.ada.proj.repository.TradeLogRepository;
import com.ada.proj.repository.UserRepository;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TradeServiceTest {

    @Test
    void purchase_food_usesCoins() {
        TradeItemRepository tradeItemRepository = mock(TradeItemRepository.class);
        TradeLogRepository tradeLogRepository = mock(TradeLogRepository.class);
        PointsService pointsService = mock(PointsService.class);
        CoinsService coinsService = mock(CoinsService.class);
        UserRepository userRepository = mock(UserRepository.class);

        TradeItem item = TradeItem.builder()
                .itemUuid("item-1")
                .name("샌드위치")
                .price(10)
                .active(true)
                .category(TradeCategory.FOOD)
                .stock(0)
                .build();

        when(tradeItemRepository.findByItemUuidForUpdate("item-1")).thenReturn(Optional.of(item));
        when(tradeLogRepository.save(any(TradeLog.class))).thenAnswer(inv -> inv.getArgument(0));

        UserCoins coinTx = UserCoins.builder()
                .coinUuid("coin-tx")
                .userUuid("u1")
                .changeType(PointChangeType.USE)
                .coins(-20)
                .balanceAfter(80)
                .build();
        when(coinsService.useCoins(eq("u1"), eq(20), anyString())).thenReturn(coinTx);

        TradeService tradeService = new TradeService(tradeItemRepository, tradeLogRepository, pointsService, coinsService, userRepository);

        TradePurchaseRequest req = new TradePurchaseRequest();
        req.setItemUuid("item-1");
        req.setQuantity(2);

        var result = tradeService.purchase("u1", req);

        verify(coinsService).useCoins(eq("u1"), eq(20), contains("코인"));
        verify(pointsService, never()).usePoints(anyString(), anyInt(), anyString(), any(), anyString());

        assertThat(result.getLog().getCurrency()).isNotNull();
        assertThat(result.getLog().getCoinsUuid()).isEqualTo("coin-tx");
        assertThat(result.getLog().getPointsUuid()).isNull();
    }

    @Test
    void purchase_etc_usesPoints() {
        TradeItemRepository tradeItemRepository = mock(TradeItemRepository.class);
        TradeLogRepository tradeLogRepository = mock(TradeLogRepository.class);
        PointsService pointsService = mock(PointsService.class);
        CoinsService coinsService = mock(CoinsService.class);
        UserRepository userRepository = mock(UserRepository.class);

        TradeItem item = TradeItem.builder()
                .itemUuid("item-2")
                .name("프로필 테두리")
                .price(50)
                .active(true)
                .category(TradeCategory.ETC)
                .stock(0)
                .build();

        when(tradeItemRepository.findByItemUuidForUpdate("item-2")).thenReturn(Optional.of(item));
        when(tradeLogRepository.existsByUserUuidAndItemUuid("u1", "item-2")).thenReturn(false);
        when(tradeLogRepository.save(any(TradeLog.class))).thenAnswer(inv -> inv.getArgument(0));

        UserPoints pointsTx = UserPoints.builder()
                .pointsUuid("points-tx")
                .userUuid("u1")
                .changeType(PointChangeType.USE)
                .points(-50)
                .balanceAfter(150)
                .build();
        when(pointsService.usePoints(eq("u1"), eq(50), eq("trade"), isNull(), anyString())).thenReturn(pointsTx);

        TradeService tradeService = new TradeService(tradeItemRepository, tradeLogRepository, pointsService, coinsService, userRepository);

        TradePurchaseRequest req = new TradePurchaseRequest();
        req.setItemUuid("item-2");
        req.setQuantity(1);

        var result = tradeService.purchase("u1", req);

        verify(pointsService).usePoints(eq("u1"), eq(50), eq("trade"), isNull(), contains("포인트"));
        verify(coinsService, never()).useCoins(anyString(), anyInt(), anyString());

        assertThat(result.getLog().getCoinsUuid()).isNull();
        assertThat(result.getLog().getPointsUuid()).isEqualTo("points-tx");
    }
}
