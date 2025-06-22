package com.server.telegramservice.service.kafka;

import com.server.telegramservice.dto.requests.GenerationRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class GenerationEventProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.kafka.topics.generation-requests}")
    private String generationRequestsTopic;

    public void sendGenerationRequest(String operationId, GenerationRequestDTO dto) {
        try {
            String message = objectMapper.writeValueAsString(dto);

            log.info("📨 [{}] Отправляем запрос генерации в Kafka: {}",
                    operationId, generationRequestsTopic);

            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(generationRequestsTopic, operationId, message);

            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    log.debug("✅ [{}] Сообщение отправлено в Kafka, offset: {}",
                    operationId, result.getRecordMetadata().offset());

                }
                else {
                    log.error("❌ [{}] Ошибка отправки в Kafka: {}",
                            operationId, exception.getMessage());
                }
            });

        } catch (Exception e) {
            log.error("❌ [{}] Ошибка сериализации для Kafka: {}", operationId, e.getMessage());
            throw new RuntimeException("Ошибка отправки события генерации", e);
        }
    }
}
