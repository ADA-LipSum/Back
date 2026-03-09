package com.ada.proj.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "github.oauth")
public class GitHubProperties {

    private String clientId;
    private String clientSecret;
    /**
     * 서버의 GitHub OAuth 콜백 URL (예:
     * https://api.example.com/api/auth/github/callback)
     */
    private String callbackUrl;
    /**
     * 인증 완료 후 리다이렉트할 프론트엔드 주소 (예: https://example.com)
     */
    private String frontendBaseUrl = "http://localhost:3000"; 
    /**
     * 연동 성공 시 이동할 프론트엔드 경로
     */
    private String linkSuccessPath = "/settings?github_linked=true";
    /**
     * GitHub 로그인 성공 시 이동할 프론트엔드 경로
     */
    private String loginSuccessPath = "/auth/github/success";
    /**
     * 오류 발생 시 이동할 프론트엔드 경로
     */
    private String errorPath = "/auth/github/error";

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public String getLinkSuccessPath() {
        return linkSuccessPath;
    }

    public void setLinkSuccessPath(String linkSuccessPath) {
        this.linkSuccessPath = linkSuccessPath;
    }

    public String getLoginSuccessPath() {
        return loginSuccessPath;
    }

    public void setLoginSuccessPath(String loginSuccessPath) {
        this.loginSuccessPath = loginSuccessPath;
    }

    public String getErrorPath() {
        return errorPath;
    }

    public void setErrorPath(String errorPath) {
        this.errorPath = errorPath;
    }
}
