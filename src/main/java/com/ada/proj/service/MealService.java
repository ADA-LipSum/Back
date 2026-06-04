package com.ada.proj.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private static final String BREAKFAST_CODE = "1";
    private static final String LUNCH_CODE = "2";
    private static final String DINNER_CODE = "3";

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
            log.warn("Failed to cache meal response: {}", e.getMessage());
        }
        return response;
    }

    private MealResponse fetchFromNeis(String dateStr) {
        try {
            String url = buildUrl(dateStr);
            RestTemplate restTemplate = new RestTemplate();
            String json = restTemplate.getForObject(url, String.class);
            return parseMealJson(json, dateStr);
        } catch (Exception e) {
            log.warn("Failed to fetch meal from NEIS: {}", e.getMessage());
            return MealResponse.builder().date(dateStr).build();
        }
    }

    private String buildUrl(String dateStr) {
        return neisProperties.getBaseUrl() + "/mealServiceDietInfo"
                + "?Type=json"
                + "&pIndex=1"
                + "&pSize=10"
                + "&ATPT_OFCDC_SC_CODE=" + neisProperties.getOfficeCode()
                + "&SD_SCHUL_CODE=" + neisProperties.getSchoolCode()
                + "&MLSV_YMD=" + dateStr
                + (neisProperties.getKey() != null && !neisProperties.getKey().isBlank()
                        ? "&KEY=" + neisProperties.getKey() : "");
    }

    @SuppressWarnings("unchecked")
    private MealResponse parseMealJson(String json, String dateStr) {
        if (json == null) return MealResponse.builder().date(dateStr).build();

        MealResponse.MealInfo breakfast = null;
        MealResponse.MealInfo lunch = null;
        MealResponse.MealInfo dinner = null;

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode rows = root.path("mealServiceDietInfo").path(1).path("row");
            if (rows.isMissingNode() || !rows.isArray()) {
                return MealResponse.builder().date(dateStr).build();
            }

            for (JsonNode row : rows) {
                String mealCode = row.path("MMEAL_SC_CODE").asText();
                String dishStr = row.path("DDISH_NM").asText("").replaceAll("<br/>", "\n");
                String calorie = row.path("CAL_INFO").asText("");

                List<String> menus = new ArrayList<>();
                for (String item : dishStr.split("\n")) {
                    String cleaned = item.trim().replaceAll("\\(.*?\\)", "").trim();
                    if (!cleaned.isBlank()) menus.add(cleaned);
                }

                MealResponse.MealInfo info = MealResponse.MealInfo.builder()
                        .menus(menus)
                        .calorie(calorie)
                        .build();

                switch (mealCode) {
                    case BREAKFAST_CODE -> breakfast = info;
                    case LUNCH_CODE -> lunch = info;
                    case DINNER_CODE -> dinner = info;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse NEIS meal JSON: {}", e.getMessage());
        }

        return MealResponse.builder()
                .date(dateStr)
                .breakfast(breakfast)
                .lunch(lunch)
                .dinner(dinner)
                .build();
    }
}
