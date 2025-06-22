package com.server.telegramservice.dto.responses.KieAi;

import lombok.Data;

@Data
public class KieAiImageTaskResponse {
    private String taskId;
    private String status; // pending, processing, success, failed
    private String imageUrl;
    private String message;
    private Integer progress; // 0-100
    private Long createdAt;
    private Long completedAt;
    private Object error; // Error details if failed
    private Integer cost;
}
