package com.example.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final StringRedisTemplate redisTemplate;

    private static final String TOKEN_BLACKLIST_PREFIX = "auth:blacklist:";
    private static final String USER_TOKEN_PREFIX = "auth:user:token:";
    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";

    /**
     * 将Token加入黑名单
     */
    public void addToBlacklist(String token, long expirationTime) {
        String key = TOKEN_BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "1", expirationTime, TimeUnit.MILLISECONDS);
        log.info("Token已加入黑名单");
    }

    /**
     * 检查Token是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        String key = TOKEN_BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 存储用户Token
     */
    public void storeUserToken(Long userId, String token, long expirationTime) {
        String key = USER_TOKEN_PREFIX + userId;
        redisTemplate.opsForValue().set(key, token, expirationTime, TimeUnit.MILLISECONDS);
    }

    /**
     * 获取用户Token
     */
    public String getUserToken(Long userId) {
        String key = USER_TOKEN_PREFIX + userId;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除用户Token
     */
    public void deleteUserToken(Long userId) {
        String key = USER_TOKEN_PREFIX + userId;
        redisTemplate.delete(key);
    }

    /**
     * 存储刷新令牌
     */
    public void storeRefreshToken(String refreshToken, Long userId, long expirationTime) {
        String key = REFRESH_TOKEN_PREFIX + refreshToken;
        redisTemplate.opsForValue().set(key, userId.toString(), expirationTime, TimeUnit.MILLISECONDS);
    }

    /**
     * 验证刷新令牌
     */
    public Long validateRefreshToken(String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + refreshToken;
        String userId = redisTemplate.opsForValue().get(key);
        return userId != null ? Long.valueOf(userId) : null;
    }

    /**
     * 删除刷新令牌
     */
    public void deleteRefreshToken(String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + refreshToken;
        redisTemplate.delete(key);
    }
}
