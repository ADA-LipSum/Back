package com.ada.proj.enums;

public enum NoticeTag {
    EVENT,       // 행사
    SERVICE,     // 서비스
    EMPLOYMENT,  // 취업
    OTHER;       // 기타

    public static NoticeTag from(String value) {
        if (value == null || value.isBlank()) return null;
        for (NoticeTag t : values()) {
            if (t.name().equalsIgnoreCase(value.trim())) return t;
        }
        throw new IllegalArgumentException("알 수 없는 태그: " + value);
    }

    public String getLabel() {
        return switch (this) {
            case EVENT -> "행사";
            case SERVICE -> "서비스";
            case EMPLOYMENT -> "취업";
            case OTHER -> "기타";
        };
    }
}
