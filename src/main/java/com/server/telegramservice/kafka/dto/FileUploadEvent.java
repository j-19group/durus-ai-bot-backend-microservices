package com.server.telegramservice.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileUploadEvent {
    private String redisKey;
    private String folder;
    private String originalFilename;
    private String contentType;
}
