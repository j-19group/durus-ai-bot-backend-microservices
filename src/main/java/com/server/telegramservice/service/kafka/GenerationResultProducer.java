package com.server.telegramservice.service.kafka;

import com.server.telegramservice.entity.enums.GenerationStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenerationResultProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.kafka.topics.generation-results}")
    private String generationResultsTopic;

    public void sendGenerationResult(String operationId, String resultUrl, GenerationStatus status) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("operationId", operationId);
            result.put("resultUrl", resultUrl);
            result.put("status", status);
            result.put("timestamp", System.currentTimeMillis());

            String message = objectMapper.writeValueAsString(result);

            kafkaTemplate.send(generationResultsTopic, operationId, message);

            log.info("📤 [{}] Результат отправлен в Kafka: {}", operationId, status);

        } catch (Exception e) {
            log.error("❌ [{}] Ошибка отправки результата в Kafka: {}", operationId, e.getMessage());
        }
    }
}
