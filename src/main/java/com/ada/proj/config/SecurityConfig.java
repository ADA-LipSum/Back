package com.ada.proj.config;

import java.util.List;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ada.proj.security.JwtAuthenticationFilter;
import com.ada.proj.security.RequestLoggingFilter;
import com.ada.proj.security.RestAccessDeniedHandler;
import com.ada.proj.security.RestAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RequestLoggingFilter requestLoggingFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final CorsProperties corsProperties;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
            RequestLoggingFilter requestLoggingFilter,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint,
            RestAccessDeniedHandler restAccessDeniedHandler,
            CorsProperties corsProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.requestLoggingFilter = requestLoggingFilter;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
        this.restAccessDeniedHandler = restAccessDeniedHandler;
        this.corsProperties = corsProperties;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {
                })
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        // SockJS 폴백이 iframe을 사용하므로 sameOrigin 허용
                        .frameOptions(frame -> frame.sameOrigin())
                        .contentTypeOptions(ct -> {})
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com; style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com; img-src 'self' data: blob: https:; connect-src 'self'"))
                        .referrerPolicy(referrer -> referrer
                                .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                )
                .exceptionHandling(ex -> ex
                .authenticationEntryPoint(restAuthenticationEntryPoint)
                .accessDeniedHandler(restAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(
                        "/api/auth/**",
                        "/health",
                        "/api/health",
                        "/swagger-uuid-autofill.js",
                        "/tools/**",
                        "/api/posts",
                        "/api/posts/view",
                        "/admin/**",
                        "/error",
                        "/favicon.ico",
                        // WebSocket (SockJS fallback)
                        "/ws/**",
                        "/ws/info",
                        "/ws/info/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/auth/status").permitAll()
                .requestMatchers(HttpMethod.GET,
                        "/api/auth/github/login",
                        "/api/auth/github/callback"
                ).permitAll()
                .requestMatchers(HttpMethod.GET,
                        "/api/posts/**",
                        "/api/community/posts/**",
                        "/api/community/dev/posts/**",
                        "/api/community/banners",
                        "/api/community/widget/popular-tags",
                        "/api/blog/posts/**",
                        "/api/notices/**",
                        "/api/polls/posts/**",
                        "/api/meal",
                        "/api/meal/**",
                        "/api/calendar",
                        "/api/calendar/year"
                ).permitAll()
                .requestMatchers(HttpMethod.POST,
                        "/api/auth/login",
                        "/api/auth/reissue",
                        "/api/auth/signup/teacher",
                        "/api/auth/admin/init"
                ).permitAll()
                .requestMatchers(HttpMethod.POST,
                        "/api/auth/admin/create",
                        "/api/auth/logout/all"
                ).hasRole("ADMIN")
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(
                        "/api/trade/items/**",
                        "/api/trade/items/search"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/studies/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/by-username/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/*/projects").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/*/guestbook").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/roles", "/api/roles/*").permitAll()
                .requestMatchers("/api/users/*/role").hasRole("ADMIN")
                .requestMatchers("/api/admin/s3/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/upload/**").authenticated()
                .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(requestLoggingFilter, JwtAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> allowedPatterns = corsProperties.getAllowedOriginPatterns();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedPatterns == null || allowedPatterns.isEmpty()
                ? List.of("http://localhost:*", "http://127.0.0.1:*")
                : allowedPatterns);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition", "Location", "X-Request-Id", "Set-Cookie"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
