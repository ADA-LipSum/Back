package com.ada.proj.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import com.ada.proj.dto.AdminCreateUserRequest;
import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.AuthTokenResponse;
import com.ada.proj.dto.CreateUserRequest;
import com.ada.proj.dto.CreateUserResponse;
import com.ada.proj.dto.LoginRequest;
import com.ada.proj.dto.LoginResponse;
import com.ada.proj.dto.TeacherSignupRequest;
import com.ada.proj.dto.TokenReissueRequest;
import com.ada.proj.exception.UnauthenticatedException;
import com.ada.proj.service.AuthService;
import com.ada.proj.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "로그인/인증")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    private final com.ada.proj.config.CookieProperties cookieProperties;

    public AuthController(
            AuthService authService,
            UserService userService,
            com.ada.proj.config.CookieProperties cookieProperties
    ) {
        this.authService = authService;
        this.userService = userService;
        this.cookieProperties = cookieProperties;
    }

    private ResponseCookie createCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(cookieProperties.isHttpOnly())
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path("/")
                .maxAge(cookieProperties.getMaxAge())
                .build();
    }

    @PostMapping("/login")
    @Operation(
            summary = "로그인",
            description = """
                    커스텀 ID/PW로 로그인합니다.

                    **Request Body:**
                    - `id` (필수): 로그인 ID (customId)
                    - `password` (필수): 비밀번호

                    **Response:**
                    - `tokenType`: 토큰 타입 (Bearer)
                    - `accessToken`: 발급된 access token (유효기간 1시간)
                    - `expiresIn`: access token 만료 시간 (초)
                    - `uuid`: 사용자 UUID
                    - `adminId`: 관리자 발급 ID
                    - `customId`: 커스텀 로그인 ID
                    - `userRealname`: 사용자 실명
                    - `userNickname`: 사용자 닉네임
                    - `profileImage`: 프로필 이미지 URL

                    로그인 성공 시 `refreshToken`은 HttpOnly 쿠키로 자동 설정됩니다.
                    ID 또는 비밀번호가 틀린 경우 401을 반환합니다.
                    """
    )
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(@Valid @RequestBody LoginRequest request) {

        LoginResponse res = authService.login(request);

        ResponseCookie cookie = createCookie(res.getRefreshToken());

        AuthTokenResponse body = AuthTokenResponse.builder()
                .tokenType(res.getTokenType())
                .accessToken(res.getAccessToken())
                .refreshToken(res.getRefreshToken())
                .expiresIn(res.getExpiresIn())
                .uuid(res.getUuid())
                .adminId(res.getAdminId())
                .customId(res.getCustomId())
                .userRealname(res.getUserRealname())
                .userNickname(res.getUserNickname())
                .profileImage(res.getProfileImage())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.ok(body));
    }

    @PostMapping("/reissue")
    @Operation(
            summary = "토큰 재발급",
            description = """
                    만료된 access token을 refresh token으로 재발급합니다.

                    refresh token은 아래 세 가지 방법 중 하나로 전달할 수 있습니다 (우선순위 순):
                    1. **HttpOnly 쿠키** `refreshToken` (권장 — 로그인 시 자동 설정)
                    2. **Authorization 헤더**: `Authorization: Bearer <refreshToken>`
                    3. **Request Body** `refreshToken` 필드 (위 두 방법이 없을 때만 사용)

                    **Request Body (선택):**
                    - `refreshToken`: refresh token 문자열 (쿠키/헤더로 전달한 경우 불필요)

                    **Response:**
                    - `tokenType`: 토큰 타입 (Bearer)
                    - `accessToken`: 새로 발급된 access token (유효기간 1시간)
                    - `expiresIn`: access token 만료 시간 (초)

                    재발급 성공 시 새 `refreshToken`이 HttpOnly 쿠키로 교체됩니다.
                    refresh token이 만료되었거나 폐기된 경우 401을 반환합니다. 이 경우 재로그인이 필요합니다.
                    """
    )
    public ResponseEntity<ApiResponse<AuthTokenResponse>> reissue(
            HttpServletRequest httpServletRequest,
            @RequestBody(required = false) TokenReissueRequest request) {

        // 1) Cookie (HttpOnly refreshToken)
        String refreshToken = extractRefreshTokenFromCookie(httpServletRequest);

        // 2) Authorization: Bearer <refreshToken> (compat)
        if (refreshToken == null || refreshToken.isBlank()) {
            refreshToken = extractBearerToken(httpServletRequest);
        }

        // 3) JSON body { refreshToken }
        if ((refreshToken == null || refreshToken.isBlank())
                && request != null
                && request.getRefreshToken() != null
                && !request.getRefreshToken().isBlank()) {
            refreshToken = request.getRefreshToken();
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.errorWithData("MISSING_REFRESH", "missing refresh token", null));
        }

        TokenReissueRequest effectiveRequest = new TokenReissueRequest();
        effectiveRequest.setRefreshToken(refreshToken);

        LoginResponse res = authService.reissue(effectiveRequest);

        ResponseCookie cookie = createCookie(res.getRefreshToken());

        AuthTokenResponse body = AuthTokenResponse.builder()
                .tokenType(res.getTokenType())
                .accessToken(res.getAccessToken())
                .refreshToken(res.getRefreshToken())
                .expiresIn(res.getExpiresIn())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.ok(body));
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookie != null && "refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private String extractBearerToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth == null || auth.isBlank()) {
            return null;
        }

        String prefix = "Bearer ";
        if (!auth.startsWith(prefix)) {
            return null;
        }

        String token = auth.substring(prefix.length()).trim();
        return token.isBlank() ? null : token;
    }

    @PostMapping("/logout")
    @Operation(
            summary = "로그아웃",
            description = """
                    현재 로그인된 사용자의 refresh token을 폐기하고 쿠키를 만료시킵니다.

                    **Request Body:** 없음 (Authorization 헤더의 Bearer access token으로 인증)

                    **Response:** 성공 메시지 반환 (`"logged out"`)

                    로그아웃 성공 시 `refreshToken` 쿠키가 빈 값으로 만료 처리됩니다.
                    인증되지 않은 요청은 401을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new UnauthenticatedException("Unauthenticated");
        }

        authService.logout(authentication.getName());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(cookieProperties.isHttpOnly())
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.okMessage("logged out"));
    }

    // [비활성화] POST /logout/all — 전체 강제 로그아웃은 서비스 중단 위험으로 제거됨

    @PostMapping("/signup/teacher")
    @Operation(
            summary = "선생님 회원가입",
            description = """
                    선생님 계정을 신규 등록합니다.

                    **Request Body:**
                    - `teacherId` (필수): 선생님 고유 ID (최대 50자)
                    - `userRealname` (필수): 실명 (최대 10자)
                    - `userNickname` (필수): 닉네임 (최대 10자)
                    - `customId` (필수): 로그인에 사용할 커스텀 ID (3~50자)
                    - `password` (필수): 비밀번호 (6~255자)

                    **Response:**
                    - `uuid`: 생성된 사용자 UUID
                    - `adminId`: 관리자 ID (teacherId)
                    - `customId`: 커스텀 로그인 ID
                    - `role`: 부여된 역할 (TEACHER)
                    """
    )
    public ResponseEntity<ApiResponse<CreateUserResponse>> signupTeacher(
            @Valid @RequestBody TeacherSignupRequest req) {

        var user = authService.signupTeacher(req);
        CreateUserResponse res = new CreateUserResponse(
                user.getUuid(),
                user.getAdminId(),
                user.getCustomId(),
                user.getRole()
        );

        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @PostMapping("/admin/create")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "관리자: 사용자 생성",
            description = """
                    ADMIN 권한으로 새 사용자를 생성합니다.

                    **Request Body:**
                    - `adminId` (필수): 관리자 그룹 ID (소속 기관/학교 ID)
                    - `userRealname` (필수): 실명 (최대 10자)
                    - `userNickname` (필수): 닉네임 (최대 10자)
                    - `role`: 역할 (STUDENT | TEACHER | ADMIN, 기본값: STUDENT)
                    - `customId`: 커스텀 로그인 ID (3~50자, 선택)
                    - `password`: 초기 비밀번호 (6~255자, 선택)

                    **Response:**
                    - `uuid`: 생성된 사용자 UUID
                    - `adminId`: 관리자 그룹 ID
                    - `customId`: 커스텀 로그인 ID
                    - `role`: 부여된 역할

                    ADMIN이 아닌 경우 403을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<CreateUserResponse>> createUserByAdmin(
            @Valid @RequestBody AdminCreateUserRequest req,
            Authentication authentication) {

        CreateUserRequest createReq = new CreateUserRequest();
        createReq.setAdminId(req.getAdminId());
        createReq.setUserRealname(req.getUserRealname());
        createReq.setUserNickname(req.getUserNickname());
        createReq.setRole(req.getRole());
        createReq.setCustomId(req.getCustomId());
        createReq.setPassword(req.getPassword());

        var user = userService.createUserByAdmin(createReq, authentication);
        CreateUserResponse res = new CreateUserResponse(
                user.getUuid(),
                user.getAdminId(),
                user.getCustomId(),
                user.getRole()
        );

        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @PostMapping("/admin/create/bulk")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @Operation(
            summary = "사용자 일괄 생성 (CSV) (관리자/선생님)",
            description = """
                    CSV 파일을 업로드하여 사용자를 일괄 생성합니다. ADMIN/TEACHER 전용입니다.

                    **CSV 형식:** `adminId,userRealname,userNickname[,customId,password]`
                    - 첫 번째 줄부터 바로 데이터 (헤더 없음, `#`으로 시작하는 줄은 주석)
                    - `adminId`, `userRealname`, `userNickname` 필수
                    - `customId`, `password` 선택 (password 는 6자 이상)

                    **Response:**
                    - `successCount`: 성공 건수
                    - `failedCount`: 실패 건수
                    - `failed`: 실패 목록 (line, adminId, reason)
                    """,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "일괄 생성 완료 (일부 실패 포함 가능)"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음 (ADMIN/TEACHER만 가능)")
            }
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkCreateUsers(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        Map<String, Object> result = userService.bulkCreateFromCsv(file, authentication);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/admin/init")
    @Operation(
            summary = "초기 관리자 생성",
            description = """
                    최초 관리자 계정을 생성합니다. 시스템 초기 설정 시 1회만 사용합니다.

                    **Request Body:**
                    - `adminId` (필수): 관리자 그룹 ID
                    - `userRealname` (필수): 실명 (최대 10자)
                    - `userNickname` (필수): 닉네임 (최대 10자)
                    - `role`: 역할 (기본값: ADMIN)
                    - `customId`: 커스텀 로그인 ID (3~50자, 선택)
                    - `password`: 비밀번호 (6~255자, 선택)

                    **Response:**
                    - `uuid`: 생성된 관리자 UUID
                    - `adminId`: 관리자 그룹 ID
                    - `customId`: 커스텀 로그인 ID
                    - `role`: 부여된 역할 (ADMIN)
                    """
    )
    public ResponseEntity<ApiResponse<CreateUserResponse>> initAdmin(
            @Valid @RequestBody CreateUserRequest req) {

        var user = userService.createInitialAdmin(req);
        CreateUserResponse res = new CreateUserResponse(
                user.getUuid(),
                user.getAdminId(),
                user.getCustomId(),
                user.getRole()
        );

        return ResponseEntity.ok(ApiResponse.ok(res));
    }
}
