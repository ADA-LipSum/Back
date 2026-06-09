package com.ada.proj.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.ada.proj.config.CookieProperties;
import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.AuthTokenResponse;
import com.ada.proj.dto.LoginResponse;
import com.ada.proj.dto.TokenReissueRequest;
import com.ada.proj.service.AuthService;
import com.ada.proj.service.UserService;

import jakarta.servlet.http.Cookie;

class AuthControllerTest {

    @Test
    void reissue_usesRefreshCookieAndReturnsUserFields() {
        AuthService authService = mock(AuthService.class);
        UserService userService = mock(UserService.class);
        CookieProperties cookieProperties = new CookieProperties();
        AuthController controller = new AuthController(authService, userService, cookieProperties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refreshToken", "refresh-old"));
        when(authService.reissue(any(TokenReissueRequest.class))).thenReturn(LoginResponse.builder()
                .tokenType("Bearer")
                .accessToken("access-new")
                .refreshToken("refresh-new")
                .expiresIn(600_000L)
                .uuid("uuid-001")
                .adminId("admin-id")
                .customId("custom-id")
                .userRealname("real-name")
                .userNickname("nickname")
                .profileImage("profile-image")
                .build());

        ResponseEntity<ApiResponse<AuthTokenResponse>> response = controller.reissue(request, null);

        ArgumentCaptor<TokenReissueRequest> requestCaptor = ArgumentCaptor.forClass(TokenReissueRequest.class);
        verify(authService).reissue(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getRefreshToken()).isEqualTo("refresh-old");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("요청이 성공적으로 처리되었습니다.");
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData().getUuid()).isEqualTo("uuid-001");
        assertThat(response.getBody().getData().getAdminId()).isEqualTo("admin-id");
        assertThat(response.getBody().getData().getCustomId()).isEqualTo("custom-id");
        assertThat(response.getBody().getData().getUserRealname()).isEqualTo("real-name");
        assertThat(response.getBody().getData().getUserNickname()).isEqualTo("nickname");
        assertThat(response.getBody().getData().getProfileImage()).isEqualTo("profile-image");
    }

    @Test
    void reissue_usesRefreshTokenFromBodyWithoutAccessToken() {
        AuthService authService = mock(AuthService.class);
        UserService userService = mock(UserService.class);
        AuthController controller = new AuthController(authService, userService, new CookieProperties());
        when(authService.reissue(any(TokenReissueRequest.class))).thenReturn(LoginResponse.builder()
                .refreshToken("refresh-new")
                .build());
        TokenReissueRequest body = new TokenReissueRequest();
        body.setRefreshToken("refresh-old");

        controller.reissue(new MockHttpServletRequest(), body);

        ArgumentCaptor<TokenReissueRequest> requestCaptor = ArgumentCaptor.forClass(TokenReissueRequest.class);
        verify(authService).reissue(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getRefreshToken()).isEqualTo("refresh-old");
    }

    @Test
    void logout_expiresRefreshCookieWithoutAuthentication() {
        AuthService authService = mock(AuthService.class);
        UserService userService = mock(UserService.class);
        CookieProperties cookieProperties = new CookieProperties();
        cookieProperties.setHttpOnly(true);
        cookieProperties.setSecure(true);
        cookieProperties.setSameSite("None");
        AuthController controller = new AuthController(authService, userService, cookieProperties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refreshToken", "admin-refresh"));

        ResponseEntity<ApiResponse<Void>> response = controller.logout(request, null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("refreshToken=")
                .contains("Path=/")
                .contains("Max-Age=0")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=None");
        verify(authService).logout(null, "admin-refresh");
    }
}
