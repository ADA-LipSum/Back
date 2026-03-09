package com.ada.proj.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "github.oauth")
public class GitHubProperties {

    private String clientId;
    private String clientSecret;
    private String callbackUrl;
    private String frontendBaseUrl = "http://localhost:3000";
    private String linkSuccessPath = "/settings?github_linked=true";
    private String loginSuccessPath = "/auth/github/success";
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
        this.frontendBaseUrl = frontendBaseUrl != null
                ? frontendBaseUrl.replaceAll("/+$", "")
                : null;
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
