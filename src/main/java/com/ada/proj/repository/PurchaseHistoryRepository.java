package com.ada.proj.repository;

import com.ada.proj.entity.PurchaseHistory;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PurchaseHistoryRepository extends JpaRepository<PurchaseHistory, String> {

    @Query("""
        SELECT ph FROM PurchaseHistory ph
        WHERE (:userUuid IS NULL OR ph.userUuid = :userUuid)
          AND (:itemUuid IS NULL OR ph.itemUuid = :itemUuid)
          AND (:start IS NULL OR ph.createdAt >= :start)
          AND (:end IS NULL OR ph.createdAt <= :end)
        ORDER BY ph.createdAt DESC
    """)
    List<PurchaseHistory> filter(
            @Param("userUuid") String userUuid,
            @Param("itemUuid") String itemUuid,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    // 아이템별 판매량
    @Query("""
        SELECT ph.itemUuid, COUNT(ph)
        FROM PurchaseHistory ph
        GROUP BY ph.itemUuid
    """)
    List<Object[]> countPurchasesGroupByItem();
}