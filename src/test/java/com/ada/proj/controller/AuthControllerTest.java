package com.ada.proj.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.ada.proj.config.CookieProperties;
import com.ada.proj.dto.ApiResponse;
import com.ada.proj.service.AuthService;
import com.ada.proj.service.UserService;

import jakarta.servlet.http.Cookie;

class AuthControllerTest {

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
