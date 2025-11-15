package com.ada.proj.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ada.proj.entity.TradeLog;

public interface TradeLogRepository extends JpaRepository<TradeLog, String> {
	Page<TradeLog> findByUserUuidOrderByCreatedAtDesc(String userUuid, Pageable pageable);
}
