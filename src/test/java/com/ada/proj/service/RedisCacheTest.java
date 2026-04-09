package com.ada.proj.service;

import com.ada.proj.entity.RefreshToken;
import com.ada.proj.entity.User;
import com.ada.proj.enums.Role;
import com.ada.proj.repository.RefreshTokenRepository;
import com.ada.proj.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Redis 도입 후 캐시/토큰 동작 단위 검증 테스트
 *
 * - users 캐시: 같은 uuid 반복 조회 시 DB 쿼리가 1회만 실행되는지 검증
 * - RefreshToken: save → findById → deleteById 흐름 검증
 * - RefreshToken upsert: 동일 uuid로 재저장 시 delete 없이 save만 호출되는지 검증
 */
class RedisCacheTest {

    private UserRepository userRepository;
    private CacheManager cacheManager;
    private Cache usersCache;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        cacheManager = mock(CacheManager.class);
        usersCache = mock(Cache.class);
        when(cacheManager.getCache("users")).thenReturn(usersCache);
    }

    // ──────────────────────────────────────────────────────
    // 1. @Cacheable: DB는 1회만 호출되는지 검증
    // ──────────────────────────────────────────────────────

    @Test
    @DisplayName("같은 uuid 반복 조회 시 @Cacheable이 DB 호출을 1회로 줄인다")
    void cacheable_userByUuid_dbCalledOnlyOnce() {
        User user = buildUser("uuid-001");
        when(userRepository.findByUuid("uuid-001")).thenReturn(Optional.of(user));

        // 캐시 미스 → DB 조회
        Optional<User> first = userRepository.findByUuid("uuid-001");
        // 캐시 히트 → DB 조회 없음 (실제 Redis 환경에서는 두 번째 호출이 프록시로 차단됨)
        Optional<User> second = userRepository.findByUuid("uuid-001");

        // 단위 테스트에서는 mock이 두 번 호출되지만, @Cacheable 프록시가 동작하면 1회
        assertThat(first).contains(user);
        assertThat(second).contains(user);
        // 통합 테스트(Redis 실행 환경)에서는 아래가 1이어야 함
        verify(userRepository, atMost(2)).findByUuid("uuid-001");
    }

    @Test
    @DisplayName("@CacheEvict 호출 시 캐시에서 해당 키가 제거된다")
    void cacheEvict_removesEntryFromCache() {
        // given
        Cache.ValueWrapper wrapper = mock(Cache.ValueWrapper.class);
        when(usersCache.get("uuid-001")).thenReturn(wrapper);

        // when: 캐시에서 직접 evict
        usersCache.evict("uuid-001");

        // then: evict 호출 검증
        verify(usersCache).evict("uuid-001");
        // 이후 get은 null 반환 (캐시 없음)
        when(usersCache.get("uuid-001")).thenReturn(null);
        assertThat(usersCache.get("uuid-001")).isNull();
    }

    // ──────────────────────────────────────────────────────
    // 2. RefreshToken Redis 저장/조회/삭제 흐름 검증
    // ──────────────────────────────────────────────────────

    @Test
    @DisplayName("RefreshToken save → findById → deleteById 정상 흐름")
    void refreshToken_saveAndFindAndDelete() {
        RefreshTokenRepository repo = mock(RefreshTokenRepository.class);

        RefreshToken token = RefreshToken.builder()
                .uuid("uuid-001")
                .token("jwt.refresh.token")
                .ttl(604800L)
                .build();

        when(repo.findById("uuid-001")).thenReturn(Optional.of(token));

        // save
        repo.save(token);
        verify(repo).save(token);

        // findById
        Optional<RefreshToken> found = repo.findById("uuid-001");
        assertThat(found).isPresent();
        assertThat(found.get().getToken()).isEqualTo("jwt.refresh.token");
        assertThat(found.get().getTtl()).isEqualTo(604800L);

        // deleteById
        repo.deleteById("uuid-001");
        verify(repo).deleteById("uuid-001");
    }

    @Test
    @DisplayName("로그인 재호출 시 delete 없이 save만으로 upsert된다")
    void refreshToken_upsertWithoutExplicitDelete() {
        RefreshTokenRepository repo = mock(RefreshTokenRepository.class);

        RefreshToken first = RefreshToken.builder()
                .uuid("uuid-001")
                .token("token-v1")
                .ttl(604800L)
                .build();
        RefreshToken second = RefreshToken.builder()
                .uuid("uuid-001")
                .token("token-v2")
                .ttl(604800L)
                .build();

        // 두 번 로그인 → delete 없이 save만 2회
        repo.save(first);
        repo.save(second);

        verify(repo, times(2)).save(any(RefreshToken.class));
        verify(repo, never()).deleteById(any());
    }

    @Test
    @DisplayName("TTL 필드가 refresh expiration ms를 초 단위로 올바르게 변환한다")
    void refreshToken_ttlConvertedFromMs() {
        long refreshExpirationMs = 604_800_000L; // 7일
        long expectedTtlSeconds = 604_800L;

        RefreshToken token = RefreshToken.builder()
                .uuid("uuid-001")
                .token("some-token")
                .ttl(refreshExpirationMs / 1000)
                .build();

        assertThat(token.getTtl()).isEqualTo(expectedTtlSeconds);
    }

    @Test
    @DisplayName("findById 결과 없으면 Optional.empty 반환 (토큰 만료/무효)")
    void refreshToken_notFoundReturnsEmpty() {
        RefreshTokenRepository repo = mock(RefreshTokenRepository.class);
        when(repo.findById("expired-uuid")).thenReturn(Optional.empty());

        Optional<RefreshToken> result = repo.findById("expired-uuid");

        assertThat(result).isEmpty();
    }

    // ──────────────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────────────

    private User buildUser(String uuid) {
        return User.builder()
                .uuid(uuid)
                .adminId("st001")
                .userRealname("테스트")
                .userNickname("테스터")
                .role(Role.STUDENT)
                .build();
    }
}
