package com.ada.proj.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TechSubTag {
    QUESTION,
    PROJECT,
    MEME,
    RESOURCE_SHARING,
    POLL;

    @JsonCreator
    public static TechSubTag from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().replace("-", "_").replace(" ", "_").toUpperCase();
        return switch (normalized) {
            case "QUESTION", "QNA", "Q_A", "질문" -> QUESTION;
            case "PROJECT", "프로젝트" -> PROJECT;
            case "MEME", "밈" -> MEME;
            case "RESOURCE_SHARING", "RESOURCE", "자료공유", "자료_공유" -> RESOURCE_SHARING;
            case "POLL", "VOTE", "투표" -> POLL;
            default -> TechSubTag.valueOf(normalized);
        };
    }
}
