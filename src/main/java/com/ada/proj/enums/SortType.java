package com.ada.proj.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SortType {
    LATEST, POPULAR;

    @JsonCreator
    public static SortType from(String value) {
        if (value == null || value.isBlank()) return LATEST;
        return switch (value.trim().toUpperCase()) {
            case "POPULAR", "인기순", "HOT" -> POPULAR;
            default -> LATEST;
        };
    }
}
