package com.server.telegramservice.kafka;

import com.server.telegramservice.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileDeleteConsumer {

    private final S3Service s3Service;

    @KafkaListener(topics = "file-delete-topic", groupId = "file-delete-group")
    public void consume(String key) {
        try {
            s3Service.deleteFile(key);
            log.info("File deleted from S3: {}", key);
        } catch (Exception e) {
            log.error("Failed to delete file {}", key, e);
        }
    }
}
