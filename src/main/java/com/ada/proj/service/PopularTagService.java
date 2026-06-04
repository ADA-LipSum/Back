package com.ada.proj.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.dto.PopularTagResponse;
import com.ada.proj.repository.PostRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopularTagService {

    private static final String CACHE_KEY_PREFIX = "popular:dev:tags:";
    private static final int MAX_TAGS = 8;
    private static final Duration CACHE_TTL = Duration.ofHours(2);

    private final PostRepository postRepository;
    private final StringRedisTemplate redisTemplate;

    @Transactional(readOnly = true)
    public List<PopularTagResponse> getPopularTags() {
        String cacheKey = CACHE_KEY_PREFIX + "top";
        List<String> cached = redisTemplate.opsForList().range(cacheKey, 0, -1);
        if (cached != null && !cached.isEmpty()) {
            return parseCached(cached);
        }
        return refreshAndGet();
    }

    @Scheduled(fixedDelay = 3_600_000) // 1시간
    @Transactional(readOnly = true)
    public void scheduledRefresh() {
        try {
            refreshAndGet();
            log.info("Popular dev tags cache refreshed");
        } catch (Exception e) {
            log.warn("Failed to refresh popular dev tags: {}", e.getMessage());
        }
    }

    private List<PopularTagResponse> refreshAndGet() {
        List<Object[]> rows = postRepository.findTopDevTags(MAX_TAGS);
        List<PopularTagResponse> result = rows.stream()
                .map(row -> PopularTagResponse.builder()
                        .tag((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build())
                .toList();

        String cacheKey = CACHE_KEY_PREFIX + "top";
        redisTemplate.delete(cacheKey);
        if (!result.isEmpty()) {
            List<String> values = result.stream()
                    .map(r -> r.getTag() + ":" + r.getCount())
                    .toList();
            redisTemplate.opsForList().rightPushAll(cacheKey, values);
            redisTemplate.expire(cacheKey, CACHE_TTL);
        }
        return result;
    }

    private List<PopularTagResponse> parseCached(List<String> cached) {
        List<PopularTagResponse> result = new ArrayList<>();
        for (String entry : cached) {
            int lastColon = entry.lastIndexOf(':');
            if (lastColon > 0) {
                String tag = entry.substring(0, lastColon);
                long count = Long.parseLong(entry.substring(lastColon + 1));
                result.add(new PopularTagResponse(tag, count));
            }
        }
        return result;
    }
}
