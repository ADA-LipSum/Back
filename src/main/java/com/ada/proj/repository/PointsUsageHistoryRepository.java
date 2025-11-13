package com.ada.proj.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ada.proj.entity.PointsUsageHistory;

public interface PointsUsageHistoryRepository extends JpaRepository<PointsUsageHistory, String> {
}
