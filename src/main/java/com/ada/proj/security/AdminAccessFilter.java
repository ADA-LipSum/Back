package com.ada.proj.security;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ada.proj.config.AdminAccessProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 관리자용 테스트 도구 페이지(app.admin.path)에 IP 화이트리스트를 적용한다.
 * app.admin.allowed-ips가 비어 있으면 제한하지 않는다(로컬 개발 기본값).
 */
@Component
public class AdminAccessFilter extends OncePerRequestFilter {

    private final AdminAccessProperties properties;
    private final List<IpAddressMatcher> matchers;

    public AdminAccessFilter(AdminAccessProperties properties) {
        this.properties = properties;
        this.matchers = properties.getAllowedIps().stream()
                .filter(ip -> ip != null && !ip.isBlank())
                .map(IpAddressMatcher::new)
                .collect(Collectors.toList());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (matchers.isEmpty() || !request.getRequestURI().startsWith(properties.getPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = ClientIpResolver.resolve(request);
        boolean allowed = matchers.stream().anyMatch(matcher -> matcher.matches(ip));
        if (!allowed) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
