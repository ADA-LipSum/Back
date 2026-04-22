package com.ada.proj.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.config.GitHubProperties;
import com.ada.proj.dto.ApiResponse;
import com.ada.proj.exception.UserNotFoundException;
import com.ada.proj.service.GitHubOAuthService;
import com.ada.proj.service.GitHubOAuthService.StateInfo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth/github")
@Tag(name = "GitHub OAuth")
public class GitHubAuthController {

    private final GitHubOAuthService gitHubOAuthService;
    private final GitHubProperties gitHubProperties;

    public GitHubAuthController(GitHubOAuthService gitHubOAuthService,
            GitHubProperties gitHubProperties) {
        this.gitHubOAuthService = gitHubOAuthService;
        this.gitHubProperties = gitHubProperties;
    }

    @GetMapping("/link")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "GitHub 연동 시작",
            description = """
                    로그인된 계정에 GitHub를 연동하기 위한 OAuth 인증 URL을 반환합니다.

                    **Request Body:** 없음

                    **Response:**
                    - `redirectUrl`: GitHub OAuth 인증 페이지 URL

                    반환된 URL로 사용자를 리다이렉트하면 GitHub 로그인 후 콜백이 처리됩니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Map<String, String>>> startLink(Authentication authentication) {
        String uuid = authentication.getName();
        String state = gitHubOAuthService.generateLinkState(uuid);
        String url = gitHubOAuthService.buildAuthUrl(state);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("redirectUrl", url)));
    }

    @GetMapping("/login")
    @Operation(
            summary = "GitHub 로그인 시작",
            description = """
                    GitHub OAuth 로그인을 위한 인증 URL을 반환합니다.

                    **Request Body:** 없음

                    **Response:**
                    - `redirectUrl`: GitHub OAuth 인증 페이지 URL

                    반환된 URL로 사용자를 리다이렉트하면 GitHub 로그인 후 콜백에서 access token을 발급합니다.
                    먼저 관리자 계정에 GitHub 연동(`GET /api/auth/github/link`)이 되어 있어야 합니다.
                    """
    )
    public ResponseEntity<ApiResponse<Map<String, String>>> startLogin() {
        String state = gitHubOAuthService.generateLoginState();
        String url = gitHubOAuthService.buildAuthUrl(state);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("redirectUrl", url)));
    }

    @GetMapping("/callback")
    @Operation(
            summary = "GitHub OAuth 콜백",
            description = "GitHub에서 리다이렉트되는 콜백 엔드포인트. 직접 호출하지 마세요."
    )
    public void callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            HttpServletResponse response) throws IOException {

        if (code == null || code.isBlank()) {
            response.sendRedirect(errorRedirect("GitHub 인증이 취소되었습니다."));
            return;
        }

        try {
            StateInfo info = gitHubOAuthService.verifyState(state);

            if ("link".equals(info.type())) {
                gitHubOAuthService.linkGitHub(info.uuid(), code);
                String successUrl = gitHubProperties.getFrontendBaseUrl()
                        + gitHubProperties.getLinkSuccessPath()
                        + (gitHubProperties.getLinkSuccessPath().contains("?") ? "&" : "?")
                        + "github_linked=true";
                response.sendRedirect(successUrl);

            } else {
                String accessToken = gitHubOAuthService.loginWithGitHub(code);
                String redirectUrl = gitHubProperties.getFrontendBaseUrl()
                        + gitHubProperties.getLoginSuccessPath()
                        + "?access_token=" + accessToken;
                response.sendRedirect(redirectUrl);
            }

        } catch (UserNotFoundException e) {
            response.sendRedirect(errorRedirect(e.getMessage()));
        } catch (Exception e) {
            response.sendRedirect(errorRedirect("GitHub 인증 중 오류가 발생했습니다."));
        }
    }

    @DeleteMapping("/unlink")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "GitHub 연동 해제",
            description = """
                    현재 로그인된 계정의 GitHub 연동을 해제합니다.

                    **Request Body:** 없음

                    **Response:** 성공 메시지 반환 (`"GitHub 연동이 해제되었습니다."`)

                    GitHub가 연동되어 있지 않은 경우 400을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> unlink(Authentication authentication) {
        gitHubOAuthService.unlinkGitHub(authentication.getName());
        return ResponseEntity.ok(ApiResponse.okMessage("GitHub 연동이 해제되었습니다."));
    }

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "GitHub 연동 상태 조회",
            description = """
                    현재 로그인된 계정의 GitHub 연동 여부를 확인합니다.

                    **Request Body:** 없음

                    **Response:**
                    - `githubLinked`: GitHub 연동 여부 (true | false)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> status(Authentication authentication) {
        String uuid = authentication.getName();
        boolean linked = gitHubOAuthService.isLinked(uuid);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("githubLinked", linked)));
    }

    @GetMapping("/contributions/{githubLogin}")
    @Operation(
            summary = "특정 사용자 GitHub 잔디 조회",
            description = """
                    GitHub 아이디로 해당 사용자의 contribution 데이터를 조회합니다.

                    **Path Variable:**
                    - `githubLogin`: 조회할 GitHub 사용자 아이디 (e.g. octocat)

                    **Query Parameters:**
                    - `year` (선택): 조회할 연도 (기본값: 현재 연도)

                    해당 GitHub 계정이 연동된 사용자가 없으면 404를 반환합니다.
                    """
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> contributionsByGithubLogin(
            @PathVariable String githubLogin,
            @RequestParam(required = false) Integer year) {
        Map<String, Object> data = gitHubOAuthService.getContributionsByGithubLogin(githubLogin, year);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/contributions")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "GitHub 잔디(contribution) 데이터 조회",
            description = """
                    연동된 GitHub 계정의 contribution 데이터를 반환합니다.

                    **Query Parameters:**
                    - `year` (선택): 조회할 연도 (기본값: 현재 연도)

                    **Response:**
                    - `totalContributions`: 해당 연도 총 contribution 수
                    - `weeks`: 주별 contribution 데이터 배열
                    - `contributionCalendar`: 날짜별 contribution 상세 데이터

                    GitHub가 연동되어 있지 않으면 400을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> contributions(
            Authentication authentication,
            @RequestParam(required = false) Integer year) {
        Map<String, Object> data = gitHubOAuthService.getContributions(authentication.getName(), year);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    private String errorRedirect(String message) {
        String encoded = message.replace(" ", "+");
        return gitHubProperties.getFrontendBaseUrl()
                + gitHubProperties.getErrorPath()
                + "?error=" + encoded;
    }
}
