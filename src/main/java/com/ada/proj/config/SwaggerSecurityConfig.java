package com.ada.proj.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Swagger UI / OpenAPI 문서 전용 Basic Auth 체인.
 * 메인 JWT 체인보다 먼저 적용된다(@Order(1)).
 */
@Configuration
public class SwaggerSecurityConfig {

    @Value("${swagger.user:swagger}")
    private String swaggerUser;

    @Value("${swagger.password:swagger}")
    private String swaggerPassword;

    @Bean
    @Order(1)
    public SecurityFilterChain swaggerFilterChain(HttpSecurity http) throws Exception {
        InMemoryUserDetailsManager users = swaggerUserDetailsService();

        http
                .securityMatcher(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml"
                )
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .userDetailsService(users)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    private InMemoryUserDetailsManager swaggerUserDetailsService() {
        UserDetails user = User.withUsername(swaggerUser)
                .password("{noop}" + swaggerPassword)
                .roles("SWAGGER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }
}
