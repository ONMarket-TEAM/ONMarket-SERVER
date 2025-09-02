package com.onmarket.member.service.impl;

import com.onmarket.member.service.AuthSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthSessionServiceImpl implements AuthSessionService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String AUTH_SESSION_PREFIX = "profile_auth:";
    private static final Duration AUTH_SESSION_DURATION = Duration.ofMinutes(10); // 10분

    @Override
    public String createAuthSession(String email) {
        String sessionToken = UUID.randomUUID().toString();
        String key = AUTH_SESSION_PREFIX + sessionToken;

        // Redis에 이메일과 함께 저장 (10분 TTL)
        redisTemplate.opsForValue().set(key, email, AUTH_SESSION_DURATION);

        return sessionToken;
    }

    @Override
    public String validateAuthSession(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return null;
        }
        String key = AUTH_SESSION_PREFIX + sessionToken;
        return redisTemplate.opsForValue().get(key); // null이면 만료되었거나 유효하지 않음
    }

    @Override
    public void invalidateAuthSession(String sessionToken) {
        if (sessionToken != null && !sessionToken.isBlank()) {
            String key = AUTH_SESSION_PREFIX + sessionToken;
            redisTemplate.delete(key);
        }
    }

}
