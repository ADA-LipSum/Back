package com.ada.proj;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.when;
import com.ada.proj.service.AuthService;

/**
 * Verify that default identicon avatar is assigned only once (on first login),
 * saved on the User entity, and not changed on subsequent logins.
 */
public class AuthServiceAvatarTests {

    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private JwtTokenProvider jwtTokenProvider;
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setup() {
        userRepository = Mockito.mock(UserRepository.class);
        refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
        jwtTokenProvider = Mockito.mock(JwtTokenProvider.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);

        authService = new AuthService(userRepository, refreshTokenRepository, jwtTokenProvider, passwordEncoder);

        when(jwtTokenProvider.generateAccessToken(anyString(), anyString())).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken(anyString(), anyString())).thenReturn("refresh");
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(refreshTokenRepository.findByUuid(anyString())).thenReturn(Optional.empty());
        when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void firstLogin_setsAvatarOnce_andKeepsItAfterwards() {
        // given: a new user without profile image
        User user = User.builder()
                .seq(1L)
                .uuid(java.util.UUID.randomUUID().toString())
                .adminId("teacher1")
                .customId("teacher1")
                .password("$2a$dummy") // bcrypt-like prefix so PasswordEncoder.matches branch is used
                .userRealname("홍길동")
                .userNickname("길동")
                .role(Role.TEACHER)
                .loginCount(0L)
                .build();

        when(userRepository.findByAdminId("teacher1")).thenReturn(Optional.of(user));
        when(userRepository.findByCustomId("teacher1")).thenReturn(Optional.empty());

        // when: first login
        LoginRequest req = new LoginRequest();
        req.setId("teacher1");
        req.setPassword("pw");
        LoginResponse first = authService.login(req);

        // then: avatar set and returned
        assertThat(user.getProfileImage()).isNotBlank();
        assertThat(user.getProfileImage()).contains("https://api.dicebear.com/9.x/identicon/svg?seed=");
        assertThat(first.getProfileImage()).isEqualTo(user.getProfileImage());

        String savedUrl = user.getProfileImage();

        // when: second login (same user)
        when(userRepository.findByAdminId("teacher1")).thenReturn(Optional.of(user));
        LoginResponse second = authService.login(req);

        // then: avatar is unchanged
        assertThat(user.getProfileImage()).isEqualTo(savedUrl);
        assertThat(second.getProfileImage()).isEqualTo(savedUrl);
    }
}
