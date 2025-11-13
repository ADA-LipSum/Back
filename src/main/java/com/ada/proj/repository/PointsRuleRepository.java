package com.ada.proj.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ada.proj.entity.PointsRule;

public interface PointsRuleRepository extends JpaRepository<PointsRule, Long> {
    Optional<PointsRule> findByRuleCode(String ruleCode);
}
