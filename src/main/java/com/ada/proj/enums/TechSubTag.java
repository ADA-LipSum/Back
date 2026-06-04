package com.ada.proj.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TechSubTag {
    QUESTION,
    CHAT,
    TIP,
    POLL,
    PROJECT,
    RESOURCE_SHARING;

    @JsonCreator
    public static TechSubTag from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().replace("-", "_").replace(" ", "_").toUpperCase();
        return switch (normalized) {
            case "QUESTION", "QNA", "Q_A", "질문" -> QUESTION;
            case "CHAT", "FREE", "잡담" -> CHAT;
            case "TIP", "TIPS", "팁" -> TIP;
            case "POLL", "VOTE", "투표" -> POLL;
            case "PROJECT", "프로젝트" -> PROJECT;
            case "RESOURCE_SHARING", "RESOURCE", "자료공유", "자료_공유" -> RESOURCE_SHARING;
            default -> TechSubTag.valueOf(normalized);
        };
    }
}
