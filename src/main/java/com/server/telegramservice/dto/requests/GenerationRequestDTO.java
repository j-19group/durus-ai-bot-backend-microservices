package com.server.telegramservice.dto.requests;

import com.server.telegramservice.entity.enums.Bot;
import com.server.telegramservice.entity.enums.GenerationStatus;
import com.server.telegramservice.entity.enums.MediaType;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * DTO for {@link GenerationRequest}
 */
@Data
public class GenerationRequestDTO {
    private Long id;
    private String operationId;
    private Long userId;
    private Long sessionId;
    private Long chatId;
    private String prompt;
    private MediaType mediaType;
    private String style;
    private String aspectRatio;
    private Bot bot;
    private GenerationStatus status = GenerationStatus.CREATED;
    private String resultUrl;
    private String s3Url;
    private Integer rating;
    private String negativePrompt;
    private Integer seed;
    private Double strength;
    private Integer steps;
    private String quality;
    private String errorMessage;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private Long processingTimeMs;

}