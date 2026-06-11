package com.ada.proj.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ada.proj.entity.RewardGrantLog;
import com.ada.proj.enums.RewardActionCode;

public interface RewardGrantLogRepository extends JpaRepository<RewardGrantLog, Long> {

    boolean existsByUserUuidAndActionCode(String userUuid, RewardActionCode actionCode);

    boolean existsByUserUuidAndActionCodeAndCreatedAtGreaterThanEqual(
            String userUuid, RewardActionCode actionCode, Instant createdAtFrom);

    boolean existsByUserUuidAndActionCodeAndTargetKey(
            String userUuid, RewardActionCode actionCode, String targetKey);
}
