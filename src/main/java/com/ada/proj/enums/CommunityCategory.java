package com.ada.proj.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CommunityCategory {
    CHAT,
    TECH,
    MEME,
    PROJECT_SHOWCASE;

    @JsonCreator
    public static CommunityCategory from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().replace("-", "_").replace(" ", "_").toUpperCase();
        return switch (normalized) {
            case "CHAT", "FREE", "GENERAL", "잡담" -> CHAT;
            case "TECH", "DEV", "기술" -> TECH;
            case "MEME", "밈" -> MEME;
            case "PROJECT", "PROJECT_SHOWCASE", "PROJECTSHOWCASE", "SHOWCASE", "프로젝트", "프로젝트_자랑", "프로젝트자랑" -> PROJECT_SHOWCASE;
            default -> CommunityCategory.valueOf(normalized);
        };
    }

    public static CommunityCategory fromFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().replace("-", "_").replace(" ", "_").toUpperCase();
        if ("ALL".equals(normalized) || "전체".equals(value.trim())) {
            return null;
        }
        return from(value);
    }
}
