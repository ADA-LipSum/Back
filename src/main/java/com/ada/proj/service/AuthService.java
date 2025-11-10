package com.ada.proj.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.dto.LoginRequest;
import com.ada.proj.dto.LoginResponse;
import com.ada.proj.dto.TeacherSignupRequest;
import com.ada.proj.dto.TokenReissueRequest;
import com.ada.proj.entity.RefreshToken;
import com.ada.proj.entity.Role;
import com.ada.proj.entity.User;
import com.ada.proj.repository.RefreshTokenRepository;
import com.ada.proj.repository.UserRepository;
import com.ada.proj.security.JwtTokenProvider;

@Service
@Transactional
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

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
        if (log.isInfoEnabled()) {
            log.info("[AUTH] login attempt id={}", safeId(request.getId()));
        }
        User user = findUserForLogin(request.getId());
        ensurePasswordMatchesAndUpgrade(user, request.getPassword(), request.getId());

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

        LoginResponse resp = LoginResponse.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(900_000)
                .build();
        if (log.isInfoEnabled()) {
            log.info("[AUTH] login success uuid={} role={}", safeUuid(user.getUuid()), user.getRole());
        }
        return resp;
    }

    public LoginResponse adminLogin(LoginRequest request) {
        if (log.isInfoEnabled()) {
            log.info("[AUTH] admin login attempt id={}", safeId(request.getId()));
        }
        User user = findUserForLogin(request.getId());
        ensurePasswordMatchesAndUpgrade(user, request.getPassword(), request.getId());

        // 관리자 전용 체크
        if (user.getRole() != Role.ADMIN) {
            if (log.isWarnEnabled()) {
                log.warn("[AUTH] admin login rejected: not admin uuid={} role={}", safeUuid(user.getUuid()), user.getRole());
            }
            // 일반 메시지로 응답(보안상 상세 노출 회피)
            throw new IllegalArgumentException("Invalid id or password");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUuid(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUuid(), user.getRole().name());

        refreshTokenRepository.findByUuid(user.getUuid()).ifPresent(rt -> refreshTokenRepository.deleteByUuid(user.getUuid()));

        RefreshToken entity = RefreshToken.builder()
                .uuid(user.getUuid())
                .token(refreshToken)
                .expiresAt(Instant.now().plusMillis(604800000))
                .build();
        refreshTokenRepository.save(entity);

        LoginResponse resp = LoginResponse.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(900_000)
                .build();
        if (log.isInfoEnabled()) {
            log.info("[AUTH] admin login success uuid={} role={}", safeUuid(user.getUuid()), user.getRole());
        }
        return resp;
    }

    public LoginResponse teacherLogin(LoginRequest request) {
        if (log.isInfoEnabled()) {
            log.info("[AUTH] teacher login attempt id={}", safeId(request.getId()));
        }
        User user = findUserForLogin(request.getId());
        ensurePasswordMatchesAndUpgrade(user, request.getPassword(), request.getId());

        // 선생님 전용 체크
        if (user.getRole() != Role.TEACHER) {
            if (log.isWarnEnabled()) {
                log.warn("[AUTH] teacher login rejected: not teacher uuid={} role={}", safeUuid(user.getUuid()), user.getRole());
            }
            throw new IllegalArgumentException("Invalid id or password");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUuid(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUuid(), user.getRole().name());

        refreshTokenRepository.findByUuid(user.getUuid()).ifPresent(rt -> refreshTokenRepository.deleteByUuid(user.getUuid()));

        RefreshToken entity = RefreshToken.builder()
                .uuid(user.getUuid())
                .token(refreshToken)
                .expiresAt(Instant.now().plusMillis(604800000))
                .build();
        refreshTokenRepository.save(entity);

        LoginResponse resp = LoginResponse.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(900_000)
                .build();
        if (log.isInfoEnabled()) {
            log.info("[AUTH] teacher login success uuid={} role={}", safeUuid(user.getUuid()), user.getRole());
        }
        return resp;
    }

    private User findUserForLogin(String id) {
        return userRepository.findByAdminId(id)
                .or(() -> userRepository.findByCustomId(id))
                .orElseThrow(() -> {
                    if (log.isWarnEnabled()) {
                        log.warn("[AUTH] login failed: user not found id={}", safeId(id));
                    }
                    return new IllegalArgumentException("Invalid id or password");
                });
    }

    private void ensurePasswordMatchesAndUpgrade(User user, String rawPassword, String idForLog) {
        String stored = user.getPassword();
        String legacy = user.getLegacyCustomPw();
        if (stored == null && legacy == null) {
            if (log.isWarnEnabled()) {
                log.warn("[AUTH] login failed: no credential on record id={}", safeId(idForLog));
            }
            throw new IllegalArgumentException("Invalid id or password");
        }

        String candidate = stored != null ? stored : legacy;
        boolean matched;
        if (candidate.startsWith("$2a$") || candidate.startsWith("$2b$") || candidate.startsWith("$2y$")) {
            matched = passwordEncoder.matches(rawPassword, candidate);
        } else {
            matched = rawPassword.equals(candidate);
            if (matched) {
                user.setPassword(passwordEncoder.encode(candidate));
            }
        }

        if (matched && user.getPassword() == null && user.getLegacyCustomPw() != null) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        }
        if (!matched) {
            if (log.isWarnEnabled()) {
                log.warn("[AUTH] login failed: bad password id={}", safeId(idForLog));
            }
            throw new IllegalArgumentException("Invalid id or password");
        }
    }

    public LoginResponse reissue(TokenReissueRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> {
                    if (log.isWarnEnabled()) {
                        log.warn("[AUTH] refresh failed: token not found");
                    }
                    return new IllegalArgumentException("Invalid refresh token");
                });

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            if (log.isWarnEnabled()) {
                log.warn("[AUTH] refresh failed: token expired uuid={} ", safeUuid(stored.getUuid()));
            }
            throw new IllegalArgumentException("Refresh token expired");
        }

        String uuid = jwtTokenProvider.getUuid(stored.getToken());
        String role = jwtTokenProvider.getRole(stored.getToken());

        String newAccess = jwtTokenProvider.generateAccessToken(uuid, role);
        String newRefresh = jwtTokenProvider.generateRefreshToken(uuid, role);

        stored.setToken(newRefresh);
        stored.setExpiresAt(Instant.now().plusMillis(604800000));

        LoginResponse resp = LoginResponse.builder()
                .tokenType("Bearer")
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .expiresIn(900_000)
                .build();
        if (log.isInfoEnabled()) {
            log.info("[AUTH] refresh success uuid={} ", safeUuid(uuid));
        }
        return resp;
    }

    public void logout(String uuid) {
        refreshTokenRepository.deleteByUuid(uuid);
        if (log.isInfoEnabled()) {
            log.info("[AUTH] logout uuid={}", safeUuid(uuid));
        }
    }

    /**
     * 관리자 전용: 모든 사용자의 refresh 토큰을 일괄 폐기 (사실상 전체 로그아웃)
     */
    public void globalLogout(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).noneMatch(a -> a.equals("ROLE_ADMIN"))) {
            throw new SecurityException("Forbidden");
        }
        refreshTokenRepository.deleteAll();
        if (log.isWarnEnabled()) {
            log.warn("[AUTH] global logout executed by admin uuid={}", authentication.getName());
        }
    }

    /**
     * 선생님 자가 회원가입
     */
    public User signupTeacher(TeacherSignupRequest req) {
        // teacherId 를 adminId 로 사용
        userRepository.findByAdminId(req.getTeacherId()).ifPresent(u -> {
            throw new IllegalArgumentException("teacherId already exists");
        });
        if (userRepository.existsByCustomId(req.getCustomId())) {
            throw new IllegalArgumentException("customId already exists");
        }

        User user = User.builder()
                .uuid(java.util.UUID.randomUUID().toString())
                .adminId(req.getTeacherId())
                .customId(req.getCustomId())
                .password(passwordEncoder.encode(req.getPassword()))
                .userRealname(req.getUserRealname())
                .userNickname(req.getUserNickname())
                .role(Role.TEACHER)
                .build();
        return userRepository.save(user);
    }

    private String safeId(String id) {
        if (id == null) return "";
        if (id.length() <= 2) return id;
        return id.substring(0, Math.min(2, id.length())) + "***";
    }

    private String safeUuid(String uuid) {
        if (uuid == null || uuid.length() < 4) return String.valueOf(uuid);
        return "****" + uuid.substring(uuid.length() - 4);
    }
}
