package com.ada.proj.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TradeCategory {

    // 대분류
    FOOD(null),
    ETC(null),

    // FOOD 소분류
    SNACK(FOOD),
    CANDY(FOOD),
    JUICE(FOOD),
    INSTANT(FOOD),

    // ETC 소분류
    STICKER(ETC),
    BANNER(ETC);

    private final TradeCategory parent;

    public boolean isSubCategory() {
        return parent != null;
    }

    public boolean isParentCategory() {
        return parent == null;
    }
}