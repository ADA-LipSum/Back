package com.ada.proj.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ada.proj.entity.RewardRule;
import com.ada.proj.enums.RewardActionCode;

public interface RewardRuleRepository extends JpaRepository<RewardRule, Long> {
    Optional<RewardRule> findByActionCode(RewardActionCode actionCode);
}
