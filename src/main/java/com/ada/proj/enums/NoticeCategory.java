package com.ada.proj.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum NoticeCategory {
    EVENT, SERVICE, OTHER;

    @JsonCreator
    public static NoticeCategory from(String value) {
        if (value == null || value.isBlank()) return null;
        return switch (value.trim().toUpperCase()) {
            case "EVENT", "행사" -> EVENT;
            case "SERVICE", "서비스" -> SERVICE;
            default -> OTHER;
        };
    }

    public String getLabel() {
        return switch (this) {
            case EVENT -> "행사";
            case SERVICE -> "서비스";
            case OTHER -> "기타";
        };
    }
}
