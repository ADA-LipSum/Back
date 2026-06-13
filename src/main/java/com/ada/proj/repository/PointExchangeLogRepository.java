package com.ada.proj.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ada.proj.entity.PointExchangeLog;

public interface PointExchangeLogRepository extends JpaRepository<PointExchangeLog, Long> {

    @Query("SELECT COALESCE(SUM(l.coins), 0) FROM PointExchangeLog l "
            + "WHERE l.userUuid = :userUuid AND l.createdAt >= :from")
    int sumCoinsByUserUuidAndCreatedAtGreaterThanEqual(@Param("userUuid") String userUuid, @Param("from") Instant from);
}
