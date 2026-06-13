package com.ada.proj.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ada.proj.entity.PointExchangeRate;

public interface PointExchangeRateRepository extends JpaRepository<PointExchangeRate, Long> {
}
