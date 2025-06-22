package com.server.telegramservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.telegramservice.kafka.dto.FileUploadEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploadProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RedisTemplate<String, byte[]> redisTemplate;
    private final ObjectMapper objectMapper;

    public void sendFile(MultipartFile file, String folder) throws IOException {
        String redisKey = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(redisKey, file.getBytes());

        FileUploadEvent event = new FileUploadEvent(redisKey, folder,
                file.getOriginalFilename(), file.getContentType());
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IOException("Failed to serialize event", e);
        }
        kafkaTemplate.send("file-upload-topic", payload);
    }
}
