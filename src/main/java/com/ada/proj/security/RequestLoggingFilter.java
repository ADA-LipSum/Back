package com.ada.proj.security;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private static final Set<String> SENSITIVE_QUERY_KEYS = Set.of(
            "password", "pwd", "token", "access_token", "refresh_token",
            "authorization", "api_key", "apikey", "secret", "client_secret"
    );
    private static final Set<String> SENSITIVE_HEADER_KEYS = Set.of(
            "authorization", "cookie", "set-cookie"
    );

    @Value("${logging.request.warn-threshold-ms:1000}")
    private long warnThresholdMs;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String method = request.getMethod();
        final long start = System.currentTimeMillis();
        final String requestId = resolveOrCreateRequestId(request);
        MDC.put("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            String uri = request.getRequestURI();
            String qs = request.getQueryString();
            String maskedQs = sanitizeQueryString(qs);
            String path = (maskedQs == null || maskedQs.isBlank()) ? uri : (uri + "?" + maskedQs);
            String ip = resolveClientIp(request);
            String ua = maskHeader("user-agent", request.getHeader("User-Agent"));
            String contentLen = maskHeader("content-length", request.getHeader("Content-Length"));
            int status = response.getStatus();

            // Best matching pattern like /api/users/{id}
            Object bestPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            String pattern = bestPattern != null ? bestPattern.toString() : "";

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String user = (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";

            // Put context for correlation
            MDC.put("user", user);
            MDC.put("ip", ip);

            // Skip very noisy endpoints if needed (keep minimal)
            if (!isSkippable(uri)) {
                if (duration >= warnThresholdMs) {
                    log.warn("요청 느림: id={} method={} path={} 패턴={} 상태={} 소요Ms={} 사용자={} IP={} 길이={} UA=\"{}\"",
                            requestId, method, path, pattern, status, duration, user, ip, contentLen, ua);
                } else {
                    log.info("요청: id={} method={} path={} 패턴={} 상태={} 소요Ms={} 사용자={} IP={} 길이={} UA=\"{}\"",
                            requestId, method, path, pattern, status, duration, user, ip, contentLen, ua);
                }
            }
            MDC.remove("requestId");
            MDC.remove("user");
            MDC.remove("ip");
        }
    }

    private boolean isSkippable(String uri) {
        return uri.equals("/health") || uri.equals("/api/health") ||
                uri.startsWith("/v3/api-docs") || uri.startsWith("/swagger-ui") ||
                uri.startsWith("/error") || uri.startsWith("/favicon");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // In case of multiple IPs, first is original client
            int comma = xff.indexOf(',');
            return comma > -1 ? xff.substring(0, comma).trim() : xff.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return request.getRemoteAddr();
    }

    private String safeHeader(String value) {
        if (value == null) return "";
        // keep it short to avoid log flooding
        if (value.length() > 300) return value.substring(0, 300) + "…";
        return value;
    }

    private String resolveOrCreateRequestId(HttpServletRequest request) {
        String rid = request.getHeader("X-Request-Id");
        if (rid != null && !rid.isBlank()) return rid;
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String sanitizeQueryString(String qs) {
        if (qs == null || qs.isBlank()) return qs;
        StringBuilder sb = new StringBuilder();
        String[] pairs = qs.split("&");
        for (int i = 0; i < pairs.length; i++) {
            String p = pairs[i];
            int eq = p.indexOf('=');
            String rawKey = eq >= 0 ? p.substring(0, eq) : p;
            String rawVal = eq >= 0 ? p.substring(eq + 1) : "";
            String key = urlDecode(rawKey).toLowerCase();
            String val = urlDecode(rawVal);
            if (SENSITIVE_QUERY_KEYS.contains(key)) {
                val = "****";
            }
            if (i > 0) sb.append('&');
            sb.append(rawKey).append('=');
            sb.append(val.replaceAll("[^a-zA-Z0-9._~-]", "*")); // avoid CRLF/log injection
        }
        return sb.toString();
    }

    private String urlDecode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private String maskHeader(String name, String value) {
        if (value == null) return "";
        String lower = name == null ? "" : name.toLowerCase();
        if (SENSITIVE_HEADER_KEYS.contains(lower)) return "****";
        return safeHeader(value);
    }
}
