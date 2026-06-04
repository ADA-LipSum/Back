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
import com.ada.proj.dto.AcademicCalendarResponse;
import com.ada.proj.dto.AcademicScheduleEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcademicCalendarService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final Duration CACHE_TTL = Duration.ofHours(6);

    private final NeisProperties neisProperties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AcademicCalendarResponse getMonthlyCalendar(int year, int month) {
        String cacheKey = "academic:calendar:" + year + String.format("%02d", month);

        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, AcademicCalendarResponse.class);
            } catch (Exception ignored) {
            }
        }

        AcademicCalendarResponse response = fetchFromNeis(year, month);
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache academic calendar: {}", e.getMessage());
        }
        return response;
    }

    private AcademicCalendarResponse fetchFromNeis(int year, int month) {
        try {
            LocalDate firstDay = LocalDate.of(year, month, 1);
            LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
            String from = firstDay.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String to   = lastDay.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            String url = buildUrl(from, to);
            RestTemplate restTemplate = new RestTemplate();
            String json = restTemplate.getForObject(url, String.class);
            List<AcademicScheduleEvent> events = parseJson(json);

            return AcademicCalendarResponse.builder()
                    .year(year)
                    .month(month)
                    .events(events)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to fetch academic calendar from NEIS: {}", e.getMessage());
            return AcademicCalendarResponse.builder()
                    .year(year)
                    .month(month)
                    .events(List.of())
                    .build();
        }
    }

    private String buildUrl(String fromDate, String toDate) {
        return neisProperties.getBaseUrl() + "/SchoolSchedule"
                + "?Type=json"
                + "&pIndex=1"
                + "&pSize=100"
                + "&ATPT_OFCDC_SC_CODE=" + neisProperties.getOfficeCode()
                + "&SD_SCHUL_CODE=" + neisProperties.getSchoolCode()
                + "&AA_FROM_YMD=" + fromDate
                + "&AA_TO_YMD=" + toDate
                + (neisProperties.getKey() != null && !neisProperties.getKey().isBlank()
                        ? "&KEY=" + neisProperties.getKey() : "");
    }

    private List<AcademicScheduleEvent> parseJson(String json) {
        List<AcademicScheduleEvent> events = new ArrayList<>();
        if (json == null) return events;
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode rows = root.path("SchoolSchedule").path(1).path("row");
            if (rows.isMissingNode() || !rows.isArray()) return events;

            for (JsonNode row : rows) {
                String date    = row.path("AA_YMD").asText("");
                String name    = row.path("EVENT_NM").asText("").trim();
                String content = row.path("EVENT_CNTNT").asText("").trim();

                if (!date.isBlank() && !name.isBlank()) {
                    events.add(AcademicScheduleEvent.builder()
                            .date(date)
                            .eventName(name)
                            .content(content.isBlank() ? null : content)
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse NEIS schedule JSON: {}", e.getMessage());
        }
        return events;
    }
}
