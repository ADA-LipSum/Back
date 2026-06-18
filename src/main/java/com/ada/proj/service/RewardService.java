package com.ada.proj.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.dto.RewardRuleResponse;
import com.ada.proj.dto.RewardRuleUpdateRequest;
import com.ada.proj.entity.PostVisitLog;
import com.ada.proj.entity.RewardGrantLog;
import com.ada.proj.entity.RewardRule;
import com.ada.proj.enums.RewardActionCode;
import com.ada.proj.repository.PostVisitLogRepository;
import com.ada.proj.repository.RewardGrantLogRepository;
import com.ada.proj.repository.RewardRuleRepository;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RewardService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final RewardRuleRepository ruleRepository;
    private final RewardGrantLogRepository grantLogRepository;
    private final PostVisitLogRepository postVisitLogRepository;
    private final PointsService pointsService;
    private final CoinsService coinsService;

    // 새로 추가된 RewardActionCode에 대해 기본값으로 규칙을 생성해 둔다 (관리자가 이후 조정).
    @PostConstruct
    public void seedDefaultRules() {
        for (RewardActionCode code : RewardActionCode.values()) {
            if (ruleRepository.findByActionCode(code).isEmpty()) {
                ruleRepository.save(RewardRule.builder()
                        .actionCode(code)
                        .actionName(code.getDisplayName())
                        .points(code.getDefaultPoints())
                        .coins(code.getDefaultCoins())
                        .limitType(code.getDefaultLimitType())
                        .enabled(true)
                        .description(code.getDefaultDescription())
                        .threshold(code.getDefaultThreshold())
                        .build());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<RewardRuleResponse> listRules() {
        return ruleRepository.findAll().stream()
                .map(RewardRuleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RewardRuleResponse getRule(RewardActionCode actionCode) {
        return RewardRuleResponse.from(getRuleEntity(actionCode));
    }

    @Transactional
    public RewardRuleResponse updateRule(RewardActionCode actionCode, RewardRuleUpdateRequest req) {
        RewardRule rule = getRuleEntity(actionCode);

        if (req.getPoints() != null) {
            rule.setPoints(req.getPoints());
        }
        if (req.getCoins() != null) {
            rule.setCoins(req.getCoins());
        }
        if (req.getLimitType() != null) {
            rule.setLimitType(req.getLimitType());
        }
        if (req.getEnabled() != null) {
            rule.setEnabled(req.getEnabled());
        }
        if (req.getDescription() != null) {
            rule.setDescription(req.getDescription());
        }
        if (req.getThreshold() != null) {
            rule.setThreshold(req.getThreshold());
        }

        return RewardRuleResponse.from(rule);
    }

    // 규칙이 비활성화되었거나 지급 제한(limitType)에 걸리면 조용히 건너뛴다.
    @Transactional
    public void grant(String userUuid, RewardActionCode actionCode) {
        grant(userUuid, actionCode, null);
    }

    /**
     * @param targetKey ONCE_PER_TARGET 제한에서 "대상"을 구분하는 키 (예: 방명록 대상 프로필 uuid, 프로젝트 id).
     *                   해당 제한을 사용하지 않는 행동에서는 null로 호출하면 된다.
     */
    @Transactional
    public void grant(String userUuid, RewardActionCode actionCode, String targetKey) {
        try {
            RewardRule rule = getRuleEntity(actionCode);

            if (!Boolean.TRUE.equals(rule.getEnabled()) || isLimited(userUuid, rule, targetKey)) {
                return;
            }

            String description = (rule.getDescription() != null && !rule.getDescription().isBlank())
                    ? rule.getDescription()
                    : rule.getActionName();

            if (rule.getPoints() != null && rule.getPoints() > 0) {
                pointsService.grantPoints(userUuid, rule.getPoints(), description, rule.getId());
            }
            if (rule.getCoins() != null && rule.getCoins() > 0) {
                coinsService.grantCoins(userUuid, rule.getCoins(), description);
            }

            grantLogRepository.save(RewardGrantLog.builder()
                    .userUuid(userUuid)
                    .actionCode(actionCode)
                    .points(rule.getPoints())
                    .coins(rule.getCoins())
                    .targetKey(targetKey)
                    .build());
        } catch (Exception e) {
            log.warn("리워드 지급 실패: userUuid={}, actionCode={}", userUuid, actionCode, e);
        }
    }

    /**
     * 게시물 방문을 기록하고, 사용자가 방문한 게시물 수(중복 제외)가 규칙에 설정된 threshold의
     * 배수에 도달할 때마다 POST_VISIT_MILESTONE 보상을 지급한다.
     */
    @Transactional
    public void recordPostVisit(String userUuid, String postUuid) {
        if (userUuid == null || postUuid == null) {
            return;
        }
        try {
            if (postVisitLogRepository.existsByUserUuidAndPostUuid(userUuid, postUuid)) {
                return;
            }
            postVisitLogRepository.save(PostVisitLog.builder()
                    .userUuid(userUuid)
                    .postUuid(postUuid)
                    .build());

            RewardRule rule = getRuleEntity(RewardActionCode.POST_VISIT_MILESTONE);
            int threshold = (rule.getThreshold() != null && rule.getThreshold() > 0) ? rule.getThreshold() : 10;

            long visitCount = postVisitLogRepository.countByUserUuid(userUuid);
            if (visitCount % threshold == 0) {
                grant(userUuid, RewardActionCode.POST_VISIT_MILESTONE);
            }
        } catch (Exception e) {
            log.warn("게시물 방문 기록 실패: userUuid={}, postUuid={}", userUuid, postUuid, e);
        }
    }

    private boolean isLimited(String userUuid, RewardRule rule, String targetKey) {
        return switch (rule.getLimitType()) {
            case NONE -> false;
            case ONCE -> grantLogRepository.existsByUserUuidAndActionCode(userUuid, rule.getActionCode());
            case DAILY -> grantLogRepository.existsByUserUuidAndActionCodeAndCreatedAtGreaterThanEqual(
                    userUuid, rule.getActionCode(), LocalDate.now(KST).atStartOfDay(KST).toInstant());
            case ONCE_PER_TARGET -> targetKey != null
                    && grantLogRepository.existsByUserUuidAndActionCodeAndTargetKey(userUuid, rule.getActionCode(), targetKey);
        };
    }

    private RewardRule getRuleEntity(RewardActionCode actionCode) {
        return ruleRepository.findByActionCode(actionCode)
                .orElseThrow(() -> new EntityNotFoundException("리워드 규칙을 찾을 수 없습니다: " + actionCode));
    }
}
