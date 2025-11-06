package com.ada.proj.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        boolean isPost = "POST".equalsIgnoreCase(request.getMethod());
        long start = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (isPost) {
                long duration = System.currentTimeMillis() - start;
                String uri = request.getRequestURI();
                String ip = resolveClientIp(request);
                String ua = safeHeader(request.getHeader("User-Agent"));
                String contentLen = safeHeader(request.getHeader("Content-Length"));
                int status = response.getStatus();

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String user = (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";

                log.info("[REQ] method=POST uri={} status={} durMs={} user={} ip={} len={} UA=\"{}\"",
                        uri, status, duration, user, ip, contentLen, ua);
            }
        }
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
}
