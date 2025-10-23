package com.ada.proj.service;

import com.ada.proj.dto.LoginRequest;
import com.ada.proj.dto.LoginResponse;
import com.ada.proj.dto.TokenReissueRequest;
import com.ada.proj.entity.RefreshToken;
import com.ada.proj.entity.User;
import com.ada.proj.repository.RefreshTokenRepository;
import com.ada.proj.repository.UserRepository;
import com.ada.proj.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtTokenProvider jwtTokenProvider,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        // admin_id 또는 custom_id로 조회
        User user = userRepository.findByAdminId(request.getId())
                .or(() -> userRepository.findByCustomId(request.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid id or password"));

        if (user.getCustomPw() == null) {
            throw new IllegalArgumentException("Invalid id or password");
        }
        boolean matched;
        String stored = user.getCustomPw();
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            matched = passwordEncoder.matches(request.getPassword(), stored);
        } else {
            // 초기(평문) 비밀번호 호환: 일치하면 해시로 갱신
            matched = request.getPassword().equals(stored);
            if (matched) {
                user.setCustomPw(passwordEncoder.encode(stored));
            }
        }
        if (!matched) throw new IllegalArgumentException("Invalid id or password");

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUuid(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUuid(), user.getRole().name());

        // 기존 refresh 삭제 후 저장(1인 1개 정책)
        refreshTokenRepository.findByUuid(user.getUuid()).ifPresent(rt -> refreshTokenRepository.deleteByUuid(user.getUuid()));

        RefreshToken entity = RefreshToken.builder()
                .uuid(user.getUuid())
                .token(refreshToken)
                .expiresAt(Instant.now().plusMillis(604800000)) // default 7d (동일 설정)
                .build();
        refreshTokenRepository.save(entity);

        return LoginResponse.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(900_000)
                .build();
    }

    public LoginResponse reissue(TokenReissueRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token expired");
        }

        String uuid = jwtTokenProvider.getUuid(stored.getToken());
        String role = jwtTokenProvider.getRole(stored.getToken());

        String newAccess = jwtTokenProvider.generateAccessToken(uuid, role);
        String newRefresh = jwtTokenProvider.generateRefreshToken(uuid, role);

        stored.setToken(newRefresh);
        stored.setExpiresAt(Instant.now().plusMillis(604800000));

        return LoginResponse.builder()
                .tokenType("Bearer")
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .expiresIn(900_000)
                .build();
    }

    public void logout(String uuid) {
        refreshTokenRepository.deleteByUuid(uuid);
    }
}
