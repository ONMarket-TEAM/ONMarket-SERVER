package com.onmarket.recommendation.config;

import java.util.Arrays;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableCaching
@EnableAsync
@Slf4j
public class QuickCacheConfig {

    /**
     * 기본 캐시 매니저 (의존성 추가 없이 즉시 사용)
     */
    @Bean(name = "quickCacheManager")
    @Primary
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();

        // 동시성 맵 기반 캐시 (Spring 기본 제공)
        manager.setCaches(Arrays.asList(
                new ConcurrentMapCache("userRecommendations"),
                new ConcurrentMapCache("regionPosts"),
                new ConcurrentMapCache("quickRecommendations")
        ));

        return manager;
    }

    /**
     * 캐시 에러 시 정상 동작 보장
     */
    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("캐시 조회 실패 (무시): {}", exception.getMessage());
            }
            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("캐시 저장 실패 (무시): {}", exception.getMessage());
            }
            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("캐시 삭제 실패 (무시): {}", exception.getMessage());
            }
            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("캐시 클리어 실패 (무시): {}", exception.getMessage());
            }
        };
    }

    /**
     * 비동기 실행을 위한 설정
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("Async-");
        executor.initialize();
        return executor;
    }
}
