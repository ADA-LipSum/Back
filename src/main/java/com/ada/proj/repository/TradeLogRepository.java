package com.ada.proj.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ada.proj.entity.TradeItem;
import com.ada.proj.entity.TradeLog;

public interface TradeLogRepository extends JpaRepository<TradeLog, String> {
	Page<TradeLog> findByUserUuidOrderByCreatedAtDesc(String userUuid, Pageable pageable);
	boolean existsByUserUuidAndItemUuid(String userUuid, String itemUuid);

	java.util.Optional<TradeLog> findByLogUuid(String logUuid);

	long countByCancelled(boolean cancelled);

	@org.springframework.data.jpa.repository.Query(
		"SELECT COALESCE(SUM(tl.totalPoints), 0) FROM TradeLog tl WHERE tl.currency = :currency AND tl.cancelled = false")
	long sumTotalByCurrencyAndNotCancelled(
		@org.springframework.data.repository.query.Param("currency") com.ada.proj.enums.TradeCurrency currency);

	@org.springframework.data.jpa.repository.Query(
		"SELECT tl.itemUuid, ti.name, SUM(tl.quantity), SUM(tl.totalPoints), COUNT(tl) " +
		"FROM TradeLog tl JOIN TradeItem ti ON tl.itemUuid = ti.itemUuid " +
		"WHERE tl.cancelled = false " +
		"GROUP BY tl.itemUuid, ti.name " +
		"ORDER BY SUM(tl.totalPoints) DESC")
	List<Object[]> findItemSalesStats();
}
