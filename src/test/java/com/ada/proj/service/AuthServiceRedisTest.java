package com.ada.proj.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ada.proj.config.JwtProperties;
import com.ada.proj.dto.LoginRequest;
import com.ada.proj.dto.LoginResponse;
import com.ada.proj.dto.TokenReissueRequest;
import com.ada.proj.entity.RefreshToken;
import com.ada.proj.entity.User;
import com.ada.proj.enums.Role;
import com.ada.proj.exception.TokenInvalidException;
import com.ada.proj.repository.RefreshTokenRepository;
import com.ada.proj.repository.UserRepository;
import com.ada.proj.security.JwtTokenProvider;

class AuthServiceRedisTest {

    @Test
    void login_savesRefreshTokenWithRedisTtlSeconds() {
        UserRepository userRepository = mock(UserRepository.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setAccessExpirationMs(600_000L);
        jwtProperties.setRefreshExpirationMs(604_800_000L);

        User user = User.builder()
                .uuid("uuid-001")
                .adminId("admin")
                .customId("admin")
                .password("$2a$encoded")
                .role(Role.ADMIN)
                .loginCount(0L)
                .build();

        when(userRepository.findByAdminId("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pw", "$2a$encoded")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken("uuid-001", "ADMIN")).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken("uuid-001", "ADMIN")).thenReturn("refresh-token");

        AuthService authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                jwtTokenProvider,
                passwordEncoder,
                jwtProperties);

        LoginRequest request = new LoginRequest();
        request.setId("admin");
        request.setPassword("pw");

        LoginResponse response = authService.login(request);

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(tokenCaptor.getValue().getUuid()).isEqualTo("uuid-001");
        assertThat(tokenCaptor.getValue().getToken()).isEqualTo("refresh-token");
        assertThat(tokenCaptor.getValue().getTtl()).isEqualTo(604_800L);
    }

    @Test
    void reissue_updatesStoredRefreshToken() {
        UserRepository userRepository = mock(UserRepository.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setAccessExpirationMs(600_000L);
        jwtProperties.setRefreshExpirationMs(604_800_000L);

        RefreshToken stored = RefreshToken.builder()
                .uuid("uuid-001")
                .token("refresh-old")
                .ttl(1L)
                .build();

        User user = User.builder()
                .uuid("uuid-001")
                .adminId("admin")
                .customId("admin")
                .role(Role.ADMIN)
                .loginCount(1L)
                .build();

        when(jwtTokenProvider.getUuid("refresh-old")).thenReturn("uuid-001");
        when(jwtTokenProvider.getRole("refresh-old")).thenReturn("ADMIN");
        when(jwtTokenProvider.generateAccessToken("uuid-001", "ADMIN")).thenReturn("access-new");
        when(jwtTokenProvider.generateRefreshToken("uuid-001", "ADMIN")).thenReturn("refresh-new");
        when(refreshTokenRepository.findById("uuid-001")).thenReturn(Optional.of(stored));
        when(userRepository.findByUuid("uuid-001")).thenReturn(Optional.of(user));

        AuthService authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                jwtTokenProvider,
                passwordEncoder,
                jwtProperties);

        TokenReissueRequest request = new TokenReissueRequest();
        request.setRefreshToken("refresh-old");

        LoginResponse response = authService.reissue(request);

        verify(refreshTokenRepository).save(stored);
        assertThat(stored.getToken()).isEqualTo("refresh-new");
        assertThat(stored.getTtl()).isEqualTo(604_800L);
        assertThat(response.getAccessToken()).isEqualTo("access-new");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-new");
        assertThat(response.getUuid()).isEqualTo("uuid-001");
        assertThat(response.getAdminId()).isEqualTo("admin");
        assertThat(response.getCustomId()).isEqualTo("admin");
    }

    @Test
    void reissue_rejectsMissingStoredToken() {
        UserRepository userRepository = mock(UserRepository.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setAccessExpirationMs(600_000L);
        jwtProperties.setRefreshExpirationMs(604_800_000L);

        when(jwtTokenProvider.getUuid("refresh-old")).thenReturn("uuid-001");
        when(jwtTokenProvider.getRole("refresh-old")).thenReturn("ADMIN");
        when(refreshTokenRepository.findById("uuid-001")).thenReturn(Optional.empty());

        AuthService authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                jwtTokenProvider,
                passwordEncoder,
                jwtProperties);

        TokenReissueRequest request = new TokenReissueRequest();
        request.setRefreshToken("refresh-old");

        assertThatThrownBy(() -> authService.reissue(request))
                .isInstanceOf(TokenInvalidException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void logout_revokesRefreshTokenFromCookieWithoutAccessToken() {
        UserRepository userRepository = mock(UserRepository.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtProperties jwtProperties = new JwtProperties();
        RefreshToken stored = RefreshToken.builder()
                .uuid("admin-uuid")
                .token("admin-refresh")
                .build();

        when(jwtTokenProvider.getUuid("admin-refresh")).thenReturn("admin-uuid");
        when(refreshTokenRepository.findById("admin-uuid")).thenReturn(Optional.of(stored));

        AuthService authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                jwtTokenProvider,
                passwordEncoder,
                jwtProperties);

        authService.logout(null, "admin-refresh");

        verify(refreshTokenRepository).deleteById("admin-uuid");
    }

    @Test
    void logout_doesNotRevokeRefreshTokenWhenCookieDoesNotMatchStoredToken() {
        UserRepository userRepository = mock(UserRepository.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtProperties jwtProperties = new JwtProperties();
        RefreshToken stored = RefreshToken.builder()
                .uuid("admin-uuid")
                .token("other-refresh")
                .build();

        when(jwtTokenProvider.getUuid("stale-refresh")).thenReturn("admin-uuid");
        when(refreshTokenRepository.findById("admin-uuid")).thenReturn(Optional.of(stored));

        AuthService authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                jwtTokenProvider,
                passwordEncoder,
                jwtProperties);

        authService.logout(null, "stale-refresh");

        verify(refreshTokenRepository, never()).deleteById("admin-uuid");
    }
}
