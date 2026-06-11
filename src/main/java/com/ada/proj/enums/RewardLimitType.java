package com.ada.proj.enums;

public enum RewardLimitType {
    NONE,             // 제한 없음 - 행동할 때마다 지급
    DAILY,            // 하루 1회만 지급
    ONCE,             // 사용자당 평생 1회만 지급
    ONCE_PER_TARGET   // 사용자 + 대상(targetKey) 조합당 평생 1회만 지급
}
