package com.ada.proj.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.RewardRuleResponse;
import com.ada.proj.dto.RewardRuleUpdateRequest;
import com.ada.proj.enums.RewardActionCode;
import com.ada.proj.service.RewardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/reward-rules")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
@Tag(name = "리워드 규칙", description = "행동(로그인/스터디그룹 등)에 대한 포인트·코인 자동 지급 규칙 관리 API")
public class RewardRuleController {

    private final RewardService rewardService;

    @GetMapping
    @Operation(summary = "리워드 규칙 목록 조회", description = "행동별 포인트/코인 자동 지급 규칙 전체 목록을 조회합니다.")
    public ApiResponse<List<RewardRuleResponse>> list() {
        return ApiResponse.success(rewardService.listRules());
    }

    @GetMapping("/{actionCode}")
    @Operation(summary = "리워드 규칙 단건 조회", description = "특정 행동 코드의 지급 규칙을 조회합니다.")
    public ApiResponse<RewardRuleResponse> get(@PathVariable RewardActionCode actionCode) {
        return ApiResponse.success(rewardService.getRule(actionCode));
    }

    @PutMapping("/{actionCode}")
    @Operation(
            summary = "리워드 규칙 수정",
            description = """
                    특정 행동 코드의 지급 포인트/코인, 지급 제한(NONE/DAILY/ONCE/ONCE_PER_TARGET), 활성화 여부, 설명, threshold를 수정합니다.
                    threshold는 POST_VISIT_MILESTONE처럼 'N개마다' 지급되는 행동에서 N 값을 의미합니다.
                    요청 본문에 포함(non-null)된 필드만 변경됩니다.
                    """
    )
    public ApiResponse<RewardRuleResponse> update(
            @PathVariable RewardActionCode actionCode,
            @Valid @RequestBody RewardRuleUpdateRequest req) {
        return ApiResponse.success(rewardService.updateRule(actionCode, req));
    }
}
