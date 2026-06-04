package com.ada.proj.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.MealResponse;
import com.ada.proj.service.MealService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meal")
@Tag(name = "급식", description = "NEIS Open API 연동 학교 급식 정보 — 조식·중식·석식, 메뉴 목록, 칼로리. 결과는 Redis에 12시간 캐싱.")
public class MealController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final MealService mealService;

    @GetMapping
    @Operation(
            summary = "급식 조회",
            description = """
                    특정 날짜의 급식 정보를 반환합니다. **인증 불필요.**

                    - `date` 파라미터를 생략하면 **오늘** 날짜로 조회됩니다.
                    - 결과는 Redis에 **12시간** 캐싱됩니다.
                    - NEIS API에 해당 날짜 데이터가 없으면 `breakfast`·`lunch`·`dinner` 가 `null` 로 반환됩니다.

                    **Response 구조**
                    ```json
                    {
                      "date": "20260604",
                      "breakfast": { "menus": ["현미밥", "된장국"], "calorie": "650 Kcal" },
                      "lunch":     { "menus": ["잡곡밥", "김치찌개", "..."], "calorie": "850 Kcal" },
                      "dinner":    { "menus": ["흰밥", "계란국"], "calorie": "700 Kcal" }
                    }
                    ```
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "급식 정보 반환 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "날짜 형식 오류",
                    content = @Content(examples = @ExampleObject(value = """
                            {"success":false,"errorCode":"BAD_REQUEST","message":"날짜 형식이 올바르지 않습니다. yyyyMMdd 형식으로 입력해주세요."}""")))
    })
    public ResponseEntity<?> getMeal(
            @Parameter(description = "조회 날짜 (yyyyMMdd 형식). 생략 시 오늘", example = "20260604")
            @RequestParam(required = false) String date
    ) {
        LocalDate targetDate;
        if (date == null || date.isBlank()) {
            targetDate = LocalDate.now();
        } else {
            try {
                targetDate = LocalDate.parse(date, DATE_FMT);
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.fail("날짜 형식이 올바르지 않습니다. yyyyMMdd 형식으로 입력해주세요."));
            }
        }
        return ResponseEntity.ok(ApiResponse.success(mealService.getMeal(targetDate)));
    }
}
