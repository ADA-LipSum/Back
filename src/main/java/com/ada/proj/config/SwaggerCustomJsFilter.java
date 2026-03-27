package com.ada.proj.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(1)
public class SwaggerCustomJsFilter extends OncePerRequestFilter {

    private static final String SCRIPT_TAG =
            "<script src=\"/swagger-uuid-autofill.js\"></script>";

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !uri.endsWith("/swagger-ui/index.html");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, wrapper);

        byte[] original = wrapper.getContentAsByteArray();
        String html = new String(original, StandardCharsets.UTF_8);

        if (html.contains("</body>")) {
            html = html.replace("</body>", SCRIPT_TAG + "</body>");
        }

        byte[] modified = html.getBytes(StandardCharsets.UTF_8);
        response.setContentLength(modified.length);
        response.getOutputStream().write(modified);
    }
}
