package com.server.telegramservice.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FileDeleteProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendDeleteEvent(String key) {
        kafkaTemplate.send("file-delete-topic", key);
    }
}
