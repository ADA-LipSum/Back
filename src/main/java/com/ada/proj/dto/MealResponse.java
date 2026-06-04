package com.ada.proj.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "MealResponse", description = "특정 날짜의 급식 정보 (NEIS API 연동)")
public class MealResponse {

    @Schema(description = "조회 날짜 (yyyyMMdd)", example = "20260604")
    private String date;

    @Schema(description = "조식 정보. 해당 날 조식이 없으면 null")
    private MealInfo breakfast;

    @Schema(description = "중식 정보. 해당 날 중식이 없으면 null")
    private MealInfo lunch;

    @Schema(description = "석식 정보. 해당 날 석식이 없으면 null")
    private MealInfo dinner;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "MealInfo", description = "한 끼 급식 상세")
    public static class MealInfo {

        @Schema(description = "메뉴 목록 (특수문자·알레르기 번호 제거된 순수 메뉴명)",
                example = "[\"현미밥\", \"된장찌개\", \"닭볶음\", \"배추김치\"]")
        private List<String> menus;

        @Schema(description = "칼로리 (NEIS 원문 그대로)", example = "850 Kcal")
        private String calorie;
    }
}
