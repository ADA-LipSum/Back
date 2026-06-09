package com.ada.proj.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PostBoardType {
    COMMUNITY,
    BLOG;

    @JsonCreator
    public static PostBoardType from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().replace("-", "_").replace(" ", "_").toUpperCase();
        return switch (normalized) {
            case "COMMUNITY", "커뮤니티" -> COMMUNITY;
            case "BLOG", "블로그" -> BLOG;
            default -> PostBoardType.valueOf(normalized);
        };
    }
}
