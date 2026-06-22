package com.ada.proj.enums;

import lombok.Getter;

/**
 * 포인트/코인이 자동 지급되는 행동 코드.
 * 새로운 행동을 추가하려면 enum 상수를 추가하고, 해당 행동이 발생하는 지점에서
 * RewardService.grant(userUuid, 새코드)를 호출하면 된다.
 * 지급 금액/제한/활성화 여부는 관리자 API(/api/admin/reward-rules)에서 조정한다.
 */
@Getter
public enum RewardActionCode {

    LOGIN_FIRST("최초 로그인", "최초 로그인 시 지급", 20, 0, RewardLimitType.ONCE, null),
    STUDY_GROUP_CREATE("스터디 그룹 생성", "스터디 그룹을 처음 만들었을 때 지급 (평생 1회)", 50, 20, RewardLimitType.ONCE, null),
    STUDY_GROUP_JOIN("스터디 그룹 가입", "스터디 그룹 가입이 승인되었을 때 지급", 20, 10, RewardLimitType.NONE, null),
    GUESTBOOK_WRITE("방명록 작성", "다른 사용자의 프로필에 방명록을 처음 작성했을 때 지급 (대상 프로필별 1회)", 5, 0, RewardLimitType.ONCE_PER_TARGET, null),
    GITHUB_LINK_FIRST("깃허브 최초 연동", "깃허브 계정을 최초로 연동했을 때 지급", 30, 0, RewardLimitType.ONCE, null),
    ATTENDANCE_CHECK("출석체크", "출석체크 시 지급 (1일 1회)", 5, 5, RewardLimitType.DAILY, null),
    POST_VISIT_MILESTONE("게시물 방문", "게시물을 threshold개 방문할 때마다 지급", 10, 0, RewardLimitType.NONE, 10),
    PROJECT_RANKING_SELECTED("프로젝트 순위 선정", "프로젝트가 우수 프로젝트로 선정되었을 때 작성자에게 지급 (프로젝트별 1회)", 0, 100, RewardLimitType.ONCE_PER_TARGET, null);

    private final String displayName;
    private final String defaultDescription;
    private final int defaultPoints;
    private final int defaultCoins;
    private final RewardLimitType defaultLimitType;
    private final Integer defaultThreshold;

    RewardActionCode(String displayName, String defaultDescription, int defaultPoints, int defaultCoins,
            RewardLimitType defaultLimitType, Integer defaultThreshold) {
        this.displayName = displayName;
        this.defaultDescription = defaultDescription;
        this.defaultPoints = defaultPoints;
        this.defaultCoins = defaultCoins;
        this.defaultLimitType = defaultLimitType;
        this.defaultThreshold = defaultThreshold;
    }
}
