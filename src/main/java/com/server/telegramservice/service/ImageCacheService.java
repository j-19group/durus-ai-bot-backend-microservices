package com.server.telegramservice.service;


import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageCacheService {

    private final RedisTemplate<String, byte[]> redisTemplate;

    public String store(byte[] data) {
        String key = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(key, data);
        return key;
    }

    public byte[] get(String key) {
        return (byte[]) redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
