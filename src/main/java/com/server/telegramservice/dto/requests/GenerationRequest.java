package com.server.telegramservice.dto.requests;

import com.server.telegramservice.entity.enums.GenerationStatus;
import com.server.telegramservice.entity.enums.MediaType;
import com.server.telegramservice.entity.telegram.ChatSession;
import com.server.telegramservice.entity.telegram.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "generation_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operation_id", unique = true, nullable = false)
    private String operationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "prompt", length = 2000, nullable = false)
    private String prompt;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;

    @Column(name = "style")
    private String style;

    @Column(name = "aspect_ratio")
    private String aspectRatio;

    @Column(name = "model", nullable = false)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GenerationStatus status = GenerationStatus.PENDING;

    @Column(name = "result_url")
    private String resultUrl;

    @Column(name = "s3_url")
    private String s3Url;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "negative_prompt", length = 1000)
    private String negativePrompt;

    @Column(name = "seed")
    private Integer seed;

    @Column(name = "strength")
    private Double strength;

    @Column(name = "steps")
    private Integer steps;

    @Column(name = "quality")
    private String quality;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Индексы для оптимизации поиска
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;
}
