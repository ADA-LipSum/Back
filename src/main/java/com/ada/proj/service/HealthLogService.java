package com.ada.proj.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class HealthLogService {

    private static final Path LOG_PATH = Path.of("logs", "app.log");

    public record LogSummary(Instant lastLogAt, int warnCount, int errorCount, List<String> lastLines) {}

    public LogSummary summarize(int tailLines) {
        if (!Files.exists(LOG_PATH)) {
            return new LogSummary(null, 0, 0, Collections.emptyList());
        }
        try {
            List<String> lines = Files.readAllLines(LOG_PATH);
            int warn = 0; int err = 0; Instant last = null;
            for (String line : lines) {
                if (line.contains(" WARN ")) warn++;
                if (line.contains(" ERROR ")) err++;
                // 날짜 파싱 (앞부분 yyyy-MM-dd HH:mm:ss.SSS 형태)
                try {
                    String ts = line.substring(0, 23); // 23 chars
                    // 간단 파서: '2025-11-10 11:01:44.291'
                    Instant parsed = Instant.parse(ts.replace(" ", "T") + "Z");
                    last = parsed;
                } catch (Exception ignore) {}
            }
            List<String> tail = lines.stream().skip(Math.max(0, lines.size() - tailLines)).collect(Collectors.toList());
            return new LogSummary(last, warn, err, tail);
        } catch (IOException e) {
            log.warn("[HEALTH] 로그 읽기 실패: {}", e.getMessage());
            return new LogSummary(null, 0, 0, Collections.emptyList());
        }
    }
}
