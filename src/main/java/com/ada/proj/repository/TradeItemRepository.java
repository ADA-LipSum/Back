package com.ada.proj.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ada.proj.entity.TradeItem;

import jakarta.persistence.LockModeType;

public interface TradeItemRepository extends JpaRepository<TradeItem, Long>, JpaSpecificationExecutor<TradeItem> {
    Optional<TradeItem> findByItemUuid(String itemUuid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TradeItem t where t.itemUuid = :itemUuid")
    Optional<TradeItem> findByItemUuidForUpdate(@Param("itemUuid") String itemUuid);
}
