package com.ada.proj.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.ada.proj.config.GitHubProperties;
import com.ada.proj.config.JwtProperties;
import com.ada.proj.entity.RefreshToken;
import com.ada.proj.entity.User;
import com.ada.proj.exception.ForbiddenException;
import com.ada.proj.exception.UserNotFoundException;
import com.ada.proj.repository.RefreshTokenRepository;
import com.ada.proj.repository.UserRepository;
import com.ada.proj.security.JwtTokenProvider;

/**
 * GitHub OAuth 연동 / 로그인 서비스.
 *
 * <p>
 * <b>연동 흐름</b>
 * <ol>
 * <li>로그인된 사용자가 {@code /api/auth/github/link} 요청</li>
 * <li>서버가 서명된 state(type=link, uuid=사용자 UUID)를 생성하고 GitHub OAuth URL 반환</li>
 * <li>사용자가 GitHub에서 승인 → callback URL로 리다이렉트</li>
 * <li>서버가 state 검증 후 code ↔ access_token 교환, GitHub user ID 조회</li>
 * <li>현재 사용자에 github_id 저장 → 프론트엔드 성공 페이지로 리다이렉트</li>
 * </ol>
 *
 * <p>
 * <b>GitHub 로그인 흐름</b>
 * <ol>
 * <li>비로그인 사용자가 {@code /api/auth/github/login} 요청 → GitHub OAuth URL 반환</li>
 * <li>GitHub 승인 → callback URL로 리다이렉트</li>
 * <li>서버가 code 교환 후 GitHub user ID로 사용자 조회 → JWT 발급</li>
 * </ol>
 *
 * <p>
 * <b>State 형식</b>: {@code BASE64URL(payload).HMAC_HEX}
 * <br>payload = {@code "link:{uuid}:{ts}"} 또는 {@code "login:{nonce}:{ts}"}
 * <br>HMAC 키: JWT 시크릿 (HmacSHA256). State 유효시간 5분.
 */
@Service
@Transactional
public class GitHubOAuthService {

    private static final long STATE_TTL_MS = 5 * 60 * 1000L;
    private static final String GITHUB_AUTHORIZE_URL = "https://github.com/login/oauth/authorize";
    private static final String GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String GITHUB_USER_URL = "https://api.github.com/user";

    private final GitHubProperties gitHubProperties;
    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RestTemplate restTemplate;

    public GitHubOAuthService(
            GitHubProperties gitHubProperties,
            JwtProperties jwtProperties,
            UserRepository userRepository,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenRepository refreshTokenRepository) {
        this.gitHubProperties = gitHubProperties;
        this.jwtProperties = jwtProperties;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.restTemplate = new RestTemplate();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // State 생성/검증
    // ──────────────────────────────────────────────────────────────────────────
    /**
     * 계정 연동용 signed state를 생성합니다.
     *
     * @param uuid 연동할 사용자 UUID
     */
    public String generateLinkState(String uuid) {
        String payload = "link:" + uuid + ":" + Instant.now().toEpochMilli();
        return signState(payload);
    }

    /**
     * GitHub 로그인용 signed state를 생성합니다.
     */
    public String generateLoginState() {
        String nonce = java.util.UUID.randomUUID().toString().replace("-", "");
        String payload = "login:" + nonce + ":" + Instant.now().toEpochMilli();
        return signState(payload);
    }

    /**
     * State를 검증하고 파싱된 정보를 반환합니다.
     *
     * @return {@link StateInfo}
     * @throws ForbiddenException state가 유효하지 않거나 만료된 경우
     */
    public StateInfo verifyState(String state) {
        if (state == null || state.isBlank()) {
            throw new ForbiddenException("Missing OAuth state");
        }

        int dot = state.lastIndexOf('.');
        if (dot < 0) {
            throw new ForbiddenException("Malformed OAuth state");
        }

        String encoded = state.substring(0, dot);
        String sig = state.substring(dot + 1);

        // HMAC 검증
        String expectedSig = hmacHex(encoded);
        if (!constantTimeEquals(sig, expectedSig)) {
            throw new ForbiddenException("Invalid OAuth state signature");
        }

        // 페이로드 디코딩
        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new ForbiddenException("Malformed OAuth state payload");
        }

        // 형식: type:value:ts
        String[] parts = payload.split(":", 3);
        if (parts.length != 3) {
            throw new ForbiddenException("Malformed OAuth state payload");
        }

        String type = parts[0]; // "link" or "login"
        String value = parts[1]; // uuid (link) or nonce (login)
        long ts;
        try {
            ts = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            throw new ForbiddenException("Malformed OAuth state timestamp");
        }

        if (Instant.now().toEpochMilli() - ts > STATE_TTL_MS) {
            throw new ForbiddenException("OAuth state expired. Please try again.");
        }

        return new StateInfo(type, "link".equals(type) ? value : null);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GitHub OAuth URL 생성
    // ──────────────────────────────────────────────────────────────────────────
    /**
     * GitHub OAuth 인가 URL을 생성합니다.
     */
    public String buildAuthUrl(String state) {
        return GITHUB_AUTHORIZE_URL
                + "?client_id=" + encode(gitHubProperties.getClientId())
                + "&redirect_uri=" + encode(gitHubProperties.getCallbackUrl())
                + "&scope=read:user"
                + "&state=" + encode(state);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 계정 연동
    // ──────────────────────────────────────────────────────────────────────────
    /**
     * 로그인된 사용자에 GitHub 계정을 연동합니다.
     *
     * @param uuid 연동할 사용자 UUID
     * @param code GitHub에서 받은 authorization code
     * @throws ForbiddenException 이미 다른 계정에 연동된 GitHub 계정인 경우
     */
    public void linkGitHub(String uuid, String code) {
        GitHubUserInfo info = fetchGitHubUserInfo(code);

        // 이미 다른 계정에 연동된 경우 거부
        userRepository.findByGithubId(info.id()).ifPresent(existing -> {
            if (!existing.getUuid().equals(uuid)) {
                throw new ForbiddenException("이 GitHub 계정은 이미 다른 사용자에 연동되어 있습니다.");
            }
        });

        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        user.setGithubId(info.id());
        user.setGithubLogin(info.login());
        userRepository.save(user); // @Cacheable detached 엔티티이므로 명시적 저장
    }

    /**
     * 연동된 GitHub 계정을 해제합니다.
     */
    public void unlinkGitHub(String uuid) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        if (user.getGithubId() == null) {
            throw new ForbiddenException("연동된 GitHub 계정이 없습니다.");
        }
        user.setGithubId(null);
        user.setGithubLogin(null);
        userRepository.save(user); // @Cacheable detached 엔티티이므로 명시적 저장
    }

    /**
     * 해당 계정에 GitHub가 연동되어 있는지 확인합니다.
     */
    public boolean isLinked(String uuid) {
        return userRepository.findByUuid(uuid)
                .map(user -> user.getGithubId() != null)
                .orElse(false);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GitHub 로그인 (연동 후 사용 가능)
    // ──────────────────────────────────────────────────────────────────────────
    /**
     * GitHub 로그인: GitHub user ID로 사용자를 조회하고 JWT를 발급합니다.
     *
     * @param code GitHub에서 받은 authorization code
     * @return 발급된 access token
     * @throws UserNotFoundException 연동된 계정이 없는 경우
     */
    public String loginWithGitHub(String code) {
        GitHubUserInfo info = fetchGitHubUserInfo(code);

        User user = userRepository.findByGithubId(info.id())
                .orElseThrow(() -> new UserNotFoundException(
                "연동된 계정이 없습니다. 먼저 관리자가 생성한 계정으로 로그인 후 GitHub를 연동해주세요."));

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUuid(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUuid(), user.getRole().name());

        refreshTokenRepository.findByUuid(user.getUuid())
                .ifPresent(rt -> refreshTokenRepository.deleteByUuid(user.getUuid()));

        RefreshToken entity = RefreshToken.builder()
                .uuid(user.getUuid())
                .token(refreshToken)
                .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshExpirationMs()))
                .build();
        refreshTokenRepository.save(entity);

        return accessToken;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ──────────────────────────────────────────────────────────────────────────
    /**
     * GitHub user ID + login(유저명)을 함께 반환하는 내부 DTO
     */
    private record GitHubUserInfo(String id, String login) {

    }

    /**
     * authorization code로 GitHub user ID + login을 조회합니다.
     */
    private GitHubUserInfo fetchGitHubUserInfo(String code) {
        String accessToken = exchangeCodeForToken(code);
        return getGitHubUserInfo(accessToken);
    }

    @SuppressWarnings("unchecked")
    private String exchangeCodeForToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Accept", "application/json");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", gitHubProperties.getClientId());
        body.add("client_secret", gitHubProperties.getClientSecret());
        body.add("code", code);
        body.add("redirect_uri", gitHubProperties.getCallbackUrl());

        ResponseEntity<Map> response = restTemplate.postForEntity(
                GITHUB_TOKEN_URL,
                new HttpEntity<>(body, headers),
                Map.class);

        Map<String, Object> resp = response.getBody();
        if (resp == null || !resp.containsKey("access_token")) {
            String error = resp != null ? String.valueOf(resp.get("error_description")) : "unknown";
            throw new RuntimeException("GitHub 토큰 교환 실패: " + error);
        }
        return (String) resp.get("access_token");
    }

    @SuppressWarnings("unchecked")
    private GitHubUserInfo getGitHubUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Accept", "application/vnd.github.v3+json");

        ResponseEntity<Map> response = restTemplate.exchange(
                GITHUB_USER_URL,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        Map<String, Object> user = response.getBody();
        if (user == null || !user.containsKey("id")) {
            throw new RuntimeException("GitHub 사용자 정보를 가져올 수 없습니다.");
        }
        // GitHub id는 Integer 또는 Long으로 역직렬화됨
        String id = String.valueOf(user.get("id"));
        String login = user.get("login") != null ? String.valueOf(user.get("login")) : null;
        return new GitHubUserInfo(id, login);
    }

    private String signState(String payload) {
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String sig = hmacHex(encoded);
        return encoded + "." + sig;
    }

    private String hmacHex(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("HMAC 계산 실패", e);
        }
    }

    /**
     * 타이밍 공격 방지를 위한 일정-시간 문자열 비교
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 내부 DTO
    // ──────────────────────────────────────────────────────────────────────────
    public record StateInfo(String type, String uuid) {

    }
}
