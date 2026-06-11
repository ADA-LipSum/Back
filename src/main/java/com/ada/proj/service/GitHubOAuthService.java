package com.ada.proj.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.ada.proj.config.GitHubProperties;
import com.ada.proj.config.JwtProperties;
import com.ada.proj.entity.RefreshToken;
import com.ada.proj.entity.User;
import com.ada.proj.enums.RewardActionCode;
import com.ada.proj.exception.ForbiddenException;
import com.ada.proj.exception.UserNotFoundException;
import com.ada.proj.repository.RefreshTokenRepository;
import com.ada.proj.repository.UserRepository;
import com.ada.proj.security.JwtTokenProvider;

@Service
@Transactional
public class GitHubOAuthService {

    private static final long STATE_TTL_MS = 5 * 60 * 1000L;
    private static final String GITHUB_AUTHORIZE_URL = "https://github.com/login/oauth/authorize";
    private static final String GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String GITHUB_USER_URL = "https://api.github.com/user";
    private static final String GITHUB_GRAPHQL_URL = "https://api.github.com/graphql";

    private final GitHubProperties gitHubProperties;
    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RestTemplate restTemplate;
    private final CacheManager cacheManager;
    private final RewardService rewardService;

    public GitHubOAuthService(
            GitHubProperties gitHubProperties,
            JwtProperties jwtProperties,
            UserRepository userRepository,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenRepository refreshTokenRepository,
            CacheManager cacheManager,
            RewardService rewardService) {
        this.gitHubProperties = gitHubProperties;
        this.jwtProperties = jwtProperties;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.cacheManager = cacheManager;
        this.rewardService = rewardService;
        this.restTemplate = new RestTemplate();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // State 생성/검증
    // ──────────────────────────────────────────────────────────────────────────
    public String generateLinkState(String uuid) {
        String payload = "link:" + uuid + ":" + Instant.now().toEpochMilli();
        return signState(payload);
    }

    public String generateLoginState() {
        String nonce = java.util.UUID.randomUUID().toString().replace("-", "");
        String payload = "login:" + nonce + ":" + Instant.now().toEpochMilli();
        return signState(payload);
    }

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
    public String buildAuthUrl(String state) {
        return GITHUB_AUTHORIZE_URL
                + "?client_id=" + encode(gitHubProperties.getClientId())
                + "&redirect_uri=" + encode(gitHubProperties.getCallbackUrl())
                + "&scope=read:user%20repo%20read:org"
                + "&state=" + encode(state)
                + "&prompt=consent";
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 계정 연동
    // ──────────────────────────────────────────────────────────────────────────
    @CacheEvict(cacheNames = "users", key = "#uuid")
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
        boolean firstLink = user.getGithubId() == null;
        user.setGithubId(info.id());
        user.setGithubLogin(info.login());
        user.setGithubAccessToken(encryptToken(info.accessToken()));
        userRepository.save(user); // @Cacheable detached 엔티티이므로 명시적 저장

        if (firstLink) {
            rewardService.grant(uuid, RewardActionCode.GITHUB_LINK_FIRST);
        }
    }

    @CacheEvict(cacheNames = "users", key = "#uuid")
    public void unlinkGitHub(String uuid) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        if (user.getGithubId() == null) {
            throw new ForbiddenException("연동된 GitHub 계정이 없습니다.");
        }
        user.setGithubId(null);
        user.setGithubLogin(null);
        user.setGithubAccessToken(null);
        userRepository.save(user); // @Cacheable detached 엔티티이므로 명시적 저장
    }

    public boolean isLinked(String uuid) {
        return userRepository.findByUuid(uuid)
                .map(user -> user.getGithubId() != null)
                .orElse(false);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GitHub 로그인 (연동 후 사용 가능)
    // ──────────────────────────────────────────────────────────────────────────
    public String loginWithGitHub(String code) {
        GitHubUserInfo info = fetchGitHubUserInfo(code);

        User user = userRepository.findByGithubId(info.id())
                .orElseThrow(() -> new UserNotFoundException(
                "연동된 계정이 없습니다. 먼저 관리자가 생성한 계정으로 로그인 후 GitHub를 연동해주세요."));

        user.setGithubAccessToken(encryptToken(info.accessToken()));
        userRepository.save(user);

        var cache = cacheManager.getCache("users");
        if (cache != null) {
            cache.evict(user.getUuid());
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUuid(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUuid(), user.getRole().name());

        refreshTokenRepository.save(RefreshToken.builder()
                .uuid(user.getUuid())
                .token(refreshToken)
                .ttl(jwtProperties.getRefreshExpirationMs() / 1000)
                .build());

        return accessToken;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ──────────────────────────────────────────────────────────────────────────
    private record GitHubUserInfo(String id, String login, String accessToken) {

    }

    private GitHubUserInfo fetchGitHubUserInfo(String code) {
        String token = exchangeCodeForToken(code);
        Map<String, Object> raw = fetchRawGitHubUser(token);
        String id = String.valueOf(raw.get("id"));
        String login = raw.get("login") != null ? String.valueOf(raw.get("login")) : null;
        return new GitHubUserInfo(id, login, token);
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
    private Map<String, Object> fetchRawGitHubUser(String accessToken) {
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
        return user;
    }

    private String buildDateRange(Integer year) {
        if (year == null) {
            return "";
        }
        return String.format("(from: \"%d-01-01T00:00:00Z\", to: \"%d-12-31T23:59:59Z\")", year, year);
    }

    private String buildOrgDateRange(String dateRange, String orgId) {
        if (dateRange.isEmpty()) {
            return String.format("(organizationID: \"%s\")", orgId);
        }
        // (from: "...", to: "...") → (from: "...", to: "...", organizationID: "...")
        return dateRange.substring(0, dateRange.length() - 1)
                + String.format(", organizationID: \"%s\")", orgId);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeGraphQL(String query, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = restTemplate.exchange(
                GITHUB_GRAPHQL_URL, HttpMethod.POST,
                new HttpEntity<>(Map.of("query", query), headers), Map.class);
        Map<String, Object> body = response.getBody();
        if (body == null || !body.containsKey("data")) {
            throw new RuntimeException("GitHub contribution 데이터를 가져올 수 없습니다.");
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getContributionsByGithubLogin(String githubLogin, Integer year) {
        User user = userRepository.findByGithubLogin(githubLogin)
                .orElseThrow(() -> new UserNotFoundException("해당 GitHub 계정이 연동된 사용자를 찾을 수 없습니다."));
        return getContributions(user.getUuid(), year);
    }

    public Map<String, Object> getContributions(String uuid, Integer year) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        if (user.getGithubId() == null) {
            throw new ForbiddenException("GitHub 계정이 연동되어 있지 않습니다.");
        }
        String token = decryptToken(user.getGithubAccessToken());
        int resolvedYear = year != null ? year : java.time.Year.now().getValue();
        String dateRange = buildDateRange(year);

        // ── 1단계: 기본 캘린더 + 소속 org ID 목록 조회
        String firstGql = "{ viewer { login "
                + "contributionsCollection" + dateRange + " { contributionCalendar "
                + "{ totalContributions weeks { contributionDays { contributionCount date color } } } } "
                + "organizations(first: 100) { nodes { id } } } }";

        Map<String, Object> first = executeGraphQL(firstGql, token);
        Map<String, Object> viewerData = (Map<String, Object>) ((Map<String, Object>) first.get("data")).get("viewer");
        Map<String, Object> collection = (Map<String, Object>) viewerData.get("contributionsCollection");
        Map<String, Object> calendar = (Map<String, Object>) collection.get("contributionCalendar");
        List<Map<String, Object>> orgNodes = (List<Map<String, Object>>) ((Map<String, Object>) viewerData.get("organizations")).get("nodes");

        // ── 날짜별 기여 수 맵 구성 (mutable)
        Map<String, Map<String, Object>> dayMap = new java.util.LinkedHashMap<>();
        for (Map<String, Object> week : (List<Map<String, Object>>) calendar.get("weeks")) {
            for (Map<String, Object> day : (List<Map<String, Object>>) week.get("contributionDays")) {
                dayMap.put((String) day.get("date"), day);
            }
        }

        // ── 2단계: 모든 org 캔린더를 단일 GraphQL 배치 요청으로 조회 후 일별 MAX 병합
        if (!orgNodes.isEmpty()) {
            StringBuilder batchGql = new StringBuilder("{ viewer {");
            for (int i = 0; i < orgNodes.size(); i++) {
                String orgId = (String) orgNodes.get(i).get("id");
                batchGql.append(" o").append(i)
                        .append(": contributionsCollection")
                        .append(buildOrgDateRange(dateRange, orgId))
                        .append(" { contributionCalendar { weeks { contributionDays { contributionCount date } } } }");
            }
            batchGql.append(" } }");

            try {
                Map<String, Object> second = executeGraphQL(batchGql.toString(), token);
                Map<String, Object> v2 = (Map<String, Object>) ((Map<String, Object>) second.get("data")).get("viewer");
                for (int i = 0; i < orgNodes.size(); i++) {
                    Object orgResult = v2.get("o" + i);
                    if (orgResult == null) {
                        continue;
                    }
                    Map<String, Object> orgCal = (Map<String, Object>) ((Map<String, Object>) orgResult).get("contributionCalendar");
                    if (orgCal == null) {
                        continue;
                    }
                    for (Map<String, Object> week : (List<Map<String, Object>>) orgCal.get("weeks")) {
                        for (Map<String, Object> day : (List<Map<String, Object>>) week.get("contributionDays")) {
                            String date = (String) day.get("date");
                            int orgCount = ((Number) day.get("contributionCount")).intValue();
                            Map<String, Object> base = dayMap.get(date);
                            if (base != null && orgCount > ((Number) base.get("contributionCount")).intValue()) {
                                base.put("contributionCount", orgCount);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // org 데이터 조회 실패 시 기본 집계 데이터로 폴백
            }
        }

        // ── total 재계산
        int total = dayMap.values().stream()
                .mapToInt(d -> ((Number) d.get("contributionCount")).intValue())
                .sum();
        calendar.put("totalContributions", total);
        calendar.put("login", viewerData.get("login"));
        calendar.put("year", resolvedYear);
        return calendar;
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

    private static final String ENC_PREFIX = "enc:";

    private byte[] deriveAesKey() {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("AES key derivation failed", e);
        }
    }

    private String encryptToken(String plaintext) {
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(deriveAesKey(), "AES"),
                    new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return ENC_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Token encryption failed", e);
        }
    }

    private String decryptToken(String ciphertext) {
        if (ciphertext == null || !ciphertext.startsWith(ENC_PREFIX)) {
            throw new ForbiddenException("RELINK_REQUIRED");
        }
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext.substring(ENC_PREFIX.length()));
            byte[] iv = new byte[12];
            byte[] encrypted = new byte[combined.length - 12];
            System.arraycopy(combined, 0, iv, 0, 12);
            System.arraycopy(combined, 12, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(deriveAesKey(), "AES"),
                    new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ForbiddenException("RELINK_REQUIRED");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 내부 DTO
    // ──────────────────────────────────────────────────────────────────────────
    public record StateInfo(String type, String uuid) {

    }
}
