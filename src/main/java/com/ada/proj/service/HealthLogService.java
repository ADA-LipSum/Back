package com.ada.proj.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class HealthLogService {

    private final Path logPath;

    public HealthLogService(@Value("${logging.file.name:logs/app.log}") String logFile) {
        this.logPath = Path.of(logFile);
    }

    public record LogSummary(Instant lastLogAt, int warnCount, int errorCount, List<String> lastLines) {}

    public LogSummary summarize(int tailLines) {
        if (!Files.exists(logPath)) {
            log.debug("[HEALTH] 로그 파일 없음: {}", logPath.toAbsolutePath());
            return new LogSummary(null, 0, 0, Collections.emptyList());
        }
        try {
            List<String> lines = Files.readAllLines(logPath);
            int warn = 0; int err = 0; Instant last = null;
            for (String line : lines) {
                if (line.contains(" WARN ")) warn++;
                if (line.contains(" ERROR ")) err++;
                // 날짜 파싱: yyyy-MM-dd HH:mm:ss.SSS (23자, UTC 기록)
                if (line.length() >= 23) {
                    try {
                        String ts = line.substring(0, 23);
                        last = Instant.parse(ts.replace(" ", "T") + "Z");
                    } catch (Exception ignore) {}
                }
            }
            List<String> tail = lines.stream()
                    .skip(Math.max(0, lines.size() - tailLines))
                    .collect(Collectors.toList());
            return new LogSummary(last, warn, err, tail);
        } catch (IOException e) {
            log.warn("[HEALTH] 로그 읽기 실패: {}", e.getMessage());
            return new LogSummary(null, 0, 0, Collections.emptyList());
        }
    }
}
