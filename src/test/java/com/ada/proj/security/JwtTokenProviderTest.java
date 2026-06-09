package com.ada.proj.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.JwtException;

class JwtTokenProviderTest {

    private static final String SECRET = "01234567890123456789012345678901";

    @Test
    void getUuidAllowExpired_readsSubjectFromExpiredSignedToken() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider(SECRET, -1L, 60_000L);
        String expiredAccessToken = tokenProvider.generateAccessToken("user-uuid", "STUDENT");

        assertThat(tokenProvider.getUuidAllowExpired(expiredAccessToken)).isEqualTo("user-uuid");
    }

    @Test
    void getUuidAllowExpired_rejectsTokenWithInvalidSignature() {
        JwtTokenProvider issuer = new JwtTokenProvider(SECRET, -1L, 60_000L);
        JwtTokenProvider verifier = new JwtTokenProvider(
                "abcdefghijklmnopqrstuvwxyz123456",
                60_000L,
                60_000L);
        String expiredAccessToken = issuer.generateAccessToken("user-uuid", "STUDENT");

        assertThatThrownBy(() -> verifier.getUuidAllowExpired(expiredAccessToken))
                .isInstanceOf(JwtException.class);
    }
}
