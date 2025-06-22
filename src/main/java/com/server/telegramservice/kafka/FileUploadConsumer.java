package com.server.telegramservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.telegramservice.kafka.dto.FileUploadEvent;
import com.server.telegramservice.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileUploadConsumer {

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, byte[]> redisTemplate;
    private final S3Service s3Service;

    @KafkaListener(topics = "file-upload-topic", groupId = "file-upload-group")
    public void consume(String payload) {
        try {
            FileUploadEvent event = objectMapper.readValue(payload, FileUploadEvent.class);
            byte[] data = redisTemplate.opsForValue().get(event.getRedisKey());
            if (data == null) {
                log.error("No data found in Redis for key {}", event.getRedisKey());
                return;
            }
            String url = s3Service.uploadBytes(data, event.getFolder(),
                    event.getOriginalFilename(), event.getContentType());
            redisTemplate.delete(event.getRedisKey());
            log.info("File uploaded to {}", url);
        } catch (Exception e) {
            log.error("Failed to process upload payload", e);
        }
    }
}
