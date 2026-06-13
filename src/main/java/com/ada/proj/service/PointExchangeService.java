package com.ada.proj.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.dto.PointExchangeRateResponse;
import com.ada.proj.dto.PointExchangeRateUpdateRequest;
import com.ada.proj.dto.PointExchangeResponse;
import com.ada.proj.entity.PointExchangeLog;
import com.ada.proj.entity.PointExchangeRate;
import com.ada.proj.repository.PointExchangeLogRepository;
import com.ada.proj.repository.PointExchangeRateRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

// 포인트 -> 코인 단방향 교환 (코인 -> 포인트 교환은 지원하지 않음)
@Service
@RequiredArgsConstructor
public class PointExchangeService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final long RATE_ID = 1L;
    private static final int DEFAULT_POINTS_PER_COIN = 100;
    private static final int DEFAULT_MONTHLY_COIN_LIMIT = 100;

    private final PointExchangeRateRepository rateRepository;
    private final PointExchangeLogRepository exchangeLogRepository;
    private final PointsService pointsService;
    private final CoinsService coinsService;

    @PostConstruct
    public void seedDefaultRate() {
        if (rateRepository.findById(RATE_ID).isEmpty()) {
            rateRepository.save(PointExchangeRate.builder()
                    .id(RATE_ID)
                    .pointsPerCoin(DEFAULT_POINTS_PER_COIN)
                    .monthlyCoinLimit(DEFAULT_MONTHLY_COIN_LIMIT)
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public PointExchangeRateResponse getRate() {
        return toResponse(getRateEntity());
    }

    @Transactional
    public PointExchangeRateResponse updateRate(PointExchangeRateUpdateRequest req) {
        PointExchangeRate rate = getRateEntity();
        if (req.getPointsPerCoin() != null) {
            rate.setPointsPerCoin(req.getPointsPerCoin());
        }
        if (req.getMonthlyCoinLimit() != null) {
            rate.setMonthlyCoinLimit(req.getMonthlyCoinLimit());
        }
        return toResponse(rate);
    }

    @Transactional
    public PointExchangeResponse exchange(String userUuid, int points) {
        PointExchangeRate rate = getRateEntity();
        int pointsPerCoin = rate.getPointsPerCoin();
        int monthlyCoinLimit = rate.getMonthlyCoinLimit();

        int coins = points / pointsPerCoin;
        if (coins <= 0) {
            throw new IllegalArgumentException("코인으로 교환하려면 최소 " + pointsPerCoin + "포인트가 필요합니다.");
        }

        int usedThisMonth = exchangeLogRepository.sumCoinsByUserUuidAndCreatedAtGreaterThanEqual(userUuid, monthStart());
        if (usedThisMonth + coins > monthlyCoinLimit) {
            int remaining = Math.max(monthlyCoinLimit - usedThisMonth, 0);
            throw new IllegalStateException(
                    "이번 달 포인트 → 코인 교환 한도(" + monthlyCoinLimit + "코인)를 초과합니다. (이번 달 남은 한도: " + remaining + "코인)");
        }

        int usedPoints = coins * pointsPerCoin;

        pointsService.deductPoints(userUuid, usedPoints, "포인트 → 코인 교환", null);
        coinsService.grantCoins(userUuid, coins, "포인트 → 코인 교환");

        exchangeLogRepository.save(PointExchangeLog.builder()
                .userUuid(userUuid)
                .points(usedPoints)
                .coins(coins)
                .build());

        int monthlyUsedCoins = usedThisMonth + coins;

        return PointExchangeResponse.builder()
                .usedPoints(usedPoints)
                .receivedCoins(coins)
                .pointsPerCoin(pointsPerCoin)
                .pointsBalance(pointsService.getBalance(userUuid))
                .coinsBalance(coinsService.getBalance(userUuid))
                .monthlyCoinLimit(monthlyCoinLimit)
                .monthlyUsedCoins(monthlyUsedCoins)
                .monthlyRemainingCoins(Math.max(monthlyCoinLimit - monthlyUsedCoins, 0))
                .build();
    }

    private Instant monthStart() {
        return LocalDate.now(KST).withDayOfMonth(1).atStartOfDay(KST).toInstant();
    }

    private PointExchangeRateResponse toResponse(PointExchangeRate rate) {
        return PointExchangeRateResponse.builder()
                .pointsPerCoin(rate.getPointsPerCoin())
                .monthlyCoinLimit(rate.getMonthlyCoinLimit())
                .build();
    }

    private PointExchangeRate getRateEntity() {
        return rateRepository.findById(RATE_ID)
                .orElseThrow(() -> new IllegalStateException("포인트 교환 비율 설정을 찾을 수 없습니다."));
    }
}
