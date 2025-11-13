package com.ada.proj.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ada.proj.entity.PointsEventLog;

public interface PointsEventLogRepository extends JpaRepository<PointsEventLog, String> {
}
