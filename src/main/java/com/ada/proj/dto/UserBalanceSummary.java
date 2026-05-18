package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "사용자 코인/포인트 잔액 요약")
public class UserBalanceSummary {

    @Schema(description = "사용자 UUID")
    private String uuid;

    @Schema(description = "관리자 발급 ID")
    private String adminId;

    @Schema(description = "커스텀 로그인 ID")
    private String customId;

    @Schema(description = "실명")
    private String userRealname;

    @Schema(description = "닉네임")
    private String userNickname;

    @Schema(description = "코인 잔액")
    private int coinBalance;

    @Schema(description = "포인트 잔액")
    private int pointBalance;
}
