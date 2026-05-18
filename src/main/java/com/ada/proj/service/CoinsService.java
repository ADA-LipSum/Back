package com.ada.proj.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.dto.BulkGrantResultResponse;
import com.ada.proj.entity.User;
import com.ada.proj.entity.UserCoins;
import com.ada.proj.entity.UserCoinsBalance;
import com.ada.proj.enums.PointChangeType;
import com.ada.proj.enums.Role;
import com.ada.proj.repository.UserCoinsBalanceRepository;
import com.ada.proj.repository.UserCoinsRepository;
import com.ada.proj.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class CoinsService {

    private final UserCoinsRepository userCoinsRepository;
    private final UserCoinsBalanceRepository balanceRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public int getBalance(String userUuid) {
        return balanceRepository.findByUserUuid(userUuid)
                .map(UserCoinsBalance::getTotalCoins)
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public Page<UserCoins> getTransactions(String userUuid, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("seq")));
        return userCoinsRepository.findByUserUuidOrderByCreatedAtDescSeqDesc(userUuid, pageable);
    }

    @Transactional
    public UserCoins grantCoins(String userUuid, int coins, String description) {
        return applyChange(userUuid, PointChangeType.GAIN, coins, description);
    }

    @Transactional
    public UserCoins deductCoins(String userUuid, int coins, String description) {
        return applyChange(userUuid, PointChangeType.LOSS, coins, description);
    }

    @Transactional
    public UserCoins useCoins(String userUuid, int coins, String description) {
        return applyChange(userUuid, PointChangeType.USE, coins, description);
    }

    @Transactional(readOnly = true)
    public Page<UserCoins> getAdjustmentHistory(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return userCoinsRepository.findByChangeTypeInOrderByCreatedAtDescSeqDesc(
                List.of(PointChangeType.GAIN, PointChangeType.LOSS), pageable);
    }

    @Transactional
    public BulkGrantResultResponse bulkGrantByRole(Role role, PointChangeType type, int coins, String description) {
        List<User> users = userRepository.findByRole(role);
        int successCount = 0;
        for (User user : users) {
            try {
                applyChange(user.getUuid(), type, coins, description);
                successCount++;
            } catch (Exception ignored) {
            }
        }
        return new BulkGrantResultResponse(users.size(), successCount);
    }

    @Transactional
    protected UserCoins applyChange(String userUuid, PointChangeType type, int coins, String description) {
        if (userUuid == null || userUuid.isBlank()) {
            throw new IllegalArgumentException("userUuid는 필수입니다.");
        }
        if (coins <= 0) {
            throw new IllegalArgumentException("coins는 1 이상이어야 합니다.");
        }

        UserCoinsBalance balance = balanceRepository.findByUserUuidForUpdate(userUuid)
                .orElseGet(() -> {
                    UserCoinsBalance b = UserCoinsBalance.builder()
                            .userUuid(userUuid)
                            .totalCoins(0)
                            .build();
                    return balanceRepository.save(b);
                });

        int delta = switch (type) {
            case GAIN, REFUND ->
                coins;
            case LOSS, USE ->
                -coins;
        };

        long newBalanceLong = (long) balance.getTotalCoins() + delta;
        if (newBalanceLong < 0) {
            throw new IllegalArgumentException("코인이 부족합니다.");
        }
        int newBalance = (int) newBalanceLong;

        String coinUuid = UUID.randomUUID().toString();
        UserCoins tx = UserCoins.builder()
                .coinUuid(coinUuid)
                .userUuid(userUuid)
                .changeType(type)
                .coins(delta)
                .balanceAfter(newBalance)
                .description(description)
                .build();
        userCoinsRepository.save(tx);

        balance.setTotalCoins(newBalance);
        balanceRepository.save(balance);

        return tx;
    }
}
