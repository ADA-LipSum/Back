package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "관리자 전체 현황 요약")
public class AdminStatsSummaryResponse {

    @Schema(description = "전체 사용자 수")
    private long totalUsers;

    @Schema(description = "학생 수")
    private long totalStudents;

    @Schema(description = "선생님 수")
    private long totalTeachers;

    @Schema(description = "관리자 수")
    private long totalAdmins;

    @Schema(description = "전체 게시글 수")
    private long totalPosts;

    @Schema(description = "전체 댓글 수")
    private long totalComments;

    @Schema(description = "현재 활성 제재 수")
    private long activeBans;

    @Schema(description = "전체 스터디 그룹 수")
    private long totalGroups;

    @Schema(description = "전체 거래 주문 수")
    private long totalTradeOrders;

    @Schema(description = "전체 알림 수")
    private long totalNotifications;
}
