package com.ada.proj.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum MediaFilter {
    ALL, PHOTO, VIDEO, TEXT;

    @JsonCreator
    public static MediaFilter from(String value) {
        if (value == null || value.isBlank()) return ALL;
        return switch (value.trim().toUpperCase()) {
            case "PHOTO", "IMAGE", "사진" -> PHOTO;
            case "VIDEO", "영상" -> VIDEO;
            case "TEXT", "텍스트" -> TEXT;
            default -> ALL;
        };
    }
}
