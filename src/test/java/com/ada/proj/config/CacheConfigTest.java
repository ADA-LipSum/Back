package com.ada.proj.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.data.redis.serializer.SerializationException;

class CacheConfigTest {

    @Test
    void cacheErrorHandler_evictsBrokenCacheEntryOnReadFailure() {
        CacheConfig cacheConfig = new CacheConfig();
        CacheErrorHandler handler = cacheConfig.cacheErrorHandler();
        Cache cache = mock(Cache.class);

        handler.handleCacheGetError(new SerializationException("broken cache"), cache, "profile:uuid-001");

        verify(cache).evict("profile:uuid-001");
    }
}
