package com.ada.proj.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ada.proj.config.NeisProperties;
import com.ada.proj.dto.MealResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final NeisProperties neisProperties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public MealResponse getMeal(LocalDate date) {
        String dateStr = date.format(DATE_FMT);
        String cacheKey = "meal:" + dateStr;

        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, MealResponse.class);
            } catch (Exception ignored) {
            }
        }

        MealResponse response = fetchFromNeis(dateStr);
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), Duration.ofHours(12));
        } catch (Exception e) {
            log.warn("[Meal] 캐시 저장 실패: {}", e.getMessage());
        }
        return response;
    }

    private MealResponse fetchFromNeis(String dateStr) {
        String url = buildUrl(dateStr);
        log.info("[Meal] NEIS 요청 URL: {}", url);
        try {
            RestTemplate restTemplate = new RestTemplate();
            String json = restTemplate.getForObject(url, String.class);
            log.debug("[Meal] NEIS 응답 원문: {}", json);
            return parseMealJson(json, dateStr);
        } catch (Exception e) {
            log.warn("[Meal] NEIS API 호출 실패 ({}): {}", dateStr, e.getMessage());
            return MealResponse.builder().date(dateStr).build();
        }
    }

    private String buildUrl(String dateStr) {
        StringBuilder sb = new StringBuilder();
        sb.append(neisProperties.getBaseUrl()).append("/mealServiceDietInfo")
          .append("?Type=json")
          .append("&pIndex=1")
          .append("&pSize=10")
          .append("&ATPT_OFCDC_SC_CODE=").append(neisProperties.getOfficeCode())
          .append("&SD_SCHUL_CODE=").append(neisProperties.getSchoolCode())
          .append("&MLSV_YMD=").append(dateStr);
        if (neisProperties.getKey() != null && !neisProperties.getKey().isBlank()) {
            sb.append("&KEY=").append(neisProperties.getKey());
        }
        return sb.toString();
    }

    private MealResponse parseMealJson(String json, String dateStr) {
        if (json == null || json.isBlank()) {
            log.warn("[Meal] 빈 응답 ({})", dateStr);
            return MealResponse.builder().date(dateStr).build();
        }

        MealResponse.MealInfo breakfast = null;
        MealResponse.MealInfo lunch = null;
        MealResponse.MealInfo dinner = null;

        try {
            JsonNode root = objectMapper.readTree(json);

            // NEIS 오류 응답 감지
            JsonNode result = root.path("RESULT");
            if (!result.isMissingNode()) {
                log.warn("[Meal] NEIS 오류 응답 ({}): code={} message={}",
                        dateStr,
                        result.path("CODE").asText(),
                        result.path("MESSAGE").asText());
                return MealResponse.builder().date(dateStr).build();
            }

            // 정상 응답: mealServiceDietInfo[1].row
            JsonNode mealInfo = root.path("mealServiceDietInfo");
            if (mealInfo.isMissingNode() || !mealInfo.isArray() || mealInfo.size() < 2) {
                log.warn("[Meal] mealServiceDietInfo 없음 또는 구조 이상 ({})", dateStr);
                return MealResponse.builder().date(dateStr).build();
            }

            JsonNode rows = mealInfo.get(1).path("row");
            if (rows.isMissingNode() || !rows.isArray() || rows.isEmpty()) {
                log.info("[Meal] 급식 데이터 없음 ({})", dateStr);
                return MealResponse.builder().date(dateStr).build();
            }

            log.info("[Meal] {} 급식 row 수: {}", dateStr, rows.size());

            for (JsonNode row : rows) {
                String mealCode = row.path("MMEAL_SC_CODE").asText("").trim();

                // <br/> 대소문자 무시 처리
                String dishStr = row.path("DDISH_NM").asText("")
                        .replaceAll("(?i)<br\\s*/?>", "\n");
                String calorie = row.path("CAL_INFO").asText("").trim();

                List<String> menus = new ArrayList<>();
                for (String item : dishStr.split("\n")) {
                    // 알레르기 표시 제거: (1.2.5.) 형태
                    String cleaned = item.trim().replaceAll("\\([0-9.]+\\)", "").trim();
                    if (!cleaned.isBlank()) {
                        menus.add(cleaned);
                    }
                }

                log.debug("[Meal] mealCode={}, menus={}", mealCode, menus);

                MealResponse.MealInfo info = MealResponse.MealInfo.builder()
                        .menus(menus)
                        .calorie(calorie)
                        .build();

                switch (mealCode) {
                    case "1" -> breakfast = info;
                    case "2" -> lunch = info;
                    case "3" -> dinner = info;
                    default  -> log.debug("[Meal] 알 수 없는 mealCode: {}", mealCode);
                }
            }
        } catch (Exception e) {
            log.warn("[Meal] JSON 파싱 오류 ({}): {}", dateStr, e.getMessage());
        }

        return MealResponse.builder()
                .date(dateStr)
                .breakfast(breakfast)
                .lunch(lunch)
                .dinner(dinner)
                .build();
    }
}
