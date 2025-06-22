//package com.server.telegramservice.service.kafka;
//
//import com.server.telegramservice.entity.enums.GenerationStatus;
//import com.server.telegramservice.dto.requests.GenerationRequest;
//import com.server.telegramservice.dto.requests.GenerationRequestDTO;
//import com.server.telegramservice.entity.repository.GenerationRequestRepository;
//import com.server.telegramservice.service.S3Service;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.http.*;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//import com.server.telegramservice.service.ai.image.KieAiImageService;
//
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.CompletableFuture;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class GenerationConsumer {
//
//    private final GenerationRequestRepository generationRequestRepository;
//    private final S3Service s3Service;
//    private final KieAiImageService kieAiImageService;
//    private final GenerationResultProducer resultProducer;
//    private final RedisTemplate<String, byte[]> redisTemplate;
//    private final RestTemplate restTemplate ;
//
//    /**
//     * Основной метод обработки генерации
//     */
//    public void processGeneration(GenerationRequest entity, GenerationRequestDTO dto) {
//        String operationId = entity.getOperationId();
//        long startTime = System.currentTimeMillis();
//
//        log.info("🎨 [{}] Начинаем обработку генерации - Модель: {}, Тип: {}",
//                operationId, dto.getBot(), dto.getMediaType());
//
//        try {
//            // 1. Обновляем статус на RUNNING
//            updateStatus(entity, GenerationStatus.RUNNING);
//
//            // 2. Вызываем API для генерации
//            String resultUrl = callGenerationApi(dto, operationId);
//
//            if (resultUrl != null) {
//                // 3. Загружаем результат в S3
//                String s3Url = uploadResultToS3(resultUrl, dto, operationId);
//
//                // 4. Обновляем запрос в БД
//                updateSuccessfulGeneration(entity, resultUrl, s3Url, startTime);
//
//                // 5. Отправляем результат в Kafka
//                resultProducer.sendGenerationResult(operationId, s3Url, GenerationStatus.SUCCEEDED);
//
//                log.info("✅ [{}] Генерация завершена успешно за {}ms: {}",
//                        operationId, System.currentTimeMillis() - startTime, s3Url);
//
//            } else {
//                throw new RuntimeException("API вернул пустой результат");
//            }
//
//        } catch (Exception e) {
//            log.error("❌ [{}] Ошибка при обработке генерации: {}", operationId, e.getMessage(), e);
//
//            // Обновляем статус на FAILED
//            updateFailedGeneration(entity, e.getMessage(), startTime);
//
//            // Отправляем уведомление об ошибке
//            resultProducer.sendGenerationResult(operationId, null, GenerationStatus.FAILED);
//        }
//    }
//
//    /**
//     * Асинхронная обработка генерации
//     */
//    public CompletableFuture<Void> processGenerationAsync(GenerationRequest entity, GenerationRequestDTO dto) {
//        return CompletableFuture.runAsync(() -> processGeneration(entity, dto));
//    }
//
//    /**
//     * Вызов API для генерации контента
//     */
//    private String callGenerationApi(GenerationRequestDTO dto, String operationId) {
//        try {
//            log.debug("🔄 [{}] Вызываем {} API", operationId, dto.getBot());
//            kieAiImageService.generateDalleImage();
//            kieAiImageService.generateFluxImage();
//            kieAiImageService.generateStableDiffusion();
//            kieAiImageService.getImageTask(operationId);
//
//
//            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
//                String resultUrl = extractResultUrl(response.getBody(), dto);
//
//                if (resultUrl != null) {
//                    log.info("✅ [{}] API ответил успешно: {}", operationId, resultUrl);
//                    return resultUrl;
//                } else {
//                    log.error("❌ [{}] Не удалось извлечь URL из ответа API", operationId);
//                    return null;
//                }
//            } else {
//                log.error("❌ [{}] API вернул ошибку: {}", operationId, response.getStatusCode());
//                return null;
//            }
//
//        } catch (Exception e) {
//            log.error("❌ [{}] Ошибка вызова API: {}", operationId, e.getMessage(), e);
//            throw new RuntimeException("Ошибка вызова API: " + e.getMessage(), e);
//        }
//    }
//
//    /**
//     * Выполнение запроса с повторными попытками
//     */
//    private ResponseEntity<Map> executeWithRetry(String url, HttpEntity<Map<String, Object>> request, String operationId) {
//        int maxRetries = 3;
//        int delay = 1000; // 1 секунда
//
//        for (int attempt = 1; attempt <= maxRetries; attempt++) {
//            try {
//                return restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
//
//            } catch (Exception e) {
//                log.warn("⚠️ [{}] Попытка {}/{} не удалась: {}", operationId, attempt, maxRetries, e.getMessage());
//
//                if (attempt == maxRetries) {
//                    throw e;
//                }
//
//                try {
//                    Thread.sleep(delay * attempt); // Экспоненциальный backoff
//                } catch (InterruptedException ie) {
//                    Thread.currentThread().interrupt();
//                    throw new RuntimeException("Прервано во время ожидания", ie);
//                }
//            }
//        }
//
//        throw new RuntimeException("Все попытки исчерпаны");
//    }
//
//    /**
//     * Подготовка payload для разных API
//     */
//    private Map<String, Object> prepareApiPayload(GenerationRequestDTO dto) {
//        Map<String, Object> payload = new HashMap<>();
//
//        switch (dto.getModel()) {
//            case "dall-e-3":
//                payload.put("model", "dall-e-3");
//                payload.put("prompt", enhancePromptForDallE(dto.getPrompt(), dto.getStyle()));
//                payload.put("size", convertAspectRatioToDallESize(dto.getAspectRatio()));
//                payload.put("quality", dto.getQuality() != null ? dto.getQuality() : "standard");
//                payload.put("style", "vivid");
//                payload.put("n", 1);
//                break;
//
//            case "flux":
//                payload.put("model", "flux-1.1-pro");
//                payload.put("prompt", enhancePromptForFlux(dto.getPrompt(), dto.getStyle()));
//                payload.put("aspect_ratio", dto.getAspectRatio());
//                payload.put("safety_tolerance", 2);
//                payload.put("output_format", "png");
//                if (dto.getNegativePrompt() != null) {
//                    payload.put("negative_prompt", dto.getNegativePrompt());
//                }
//                if (dto.getSeed() != null) {
//                    payload.put("seed", dto.getSeed());
//                }
//                break;
//
//            case "pika":
//                payload.put("prompt", enhancePromptForPika(dto.getPrompt(), dto.getStyle()));
//                payload.put("aspect_ratio", dto.getAspectRatio());
//                payload.put("duration", 3.0);
//                payload.put("fps", 24);
//                payload.put("style", convertStyleForPika(dto.getStyle()));
//                if (dto.getSeed() != null) {
//                    payload.put("seed", dto.getSeed());
//                }
//                break;
//
//            case "runway":
//                payload.put("text_prompt", enhancePromptForRunway(dto.getPrompt(), dto.getStyle()));
//                payload.put("duration", 5);
//                payload.put("ratio", dto.getAspectRatio());
//                payload.put("watermark", false);
//                payload.put("motion_level", 3); // Средний уровень движения
//                break;
//
//            default:
//                throw new IllegalArgumentException("Неподдерживаемая модель: " + dto.getModel());
//        }
//
//        return payload;
//    }
//
//    /**
//     * Улучшение промптов для разных моделей
//     */
//    private String enhancePromptForDallE(String prompt, String style) {
//        StringBuilder enhanced = new StringBuilder(prompt);
//
//        if (style == null) {
//            enhanced.append(", high quality, detailed");
//            return enhanced.toString();
//        }
//
//        switch (style) {
//            case "realism":
//                enhanced.append(", photorealistic, high quality, detailed, professional photography, 8k resolution");
//                break;
//            case "anime":
//                enhanced.append(", anime style, manga style, cel shading, vibrant colors, detailed anime art");
//                break;
//            case "artistic":
//                enhanced.append(", artistic style, creative composition, expressive brushwork, fine art");
//                break;
//            case "grunge":
//                enhanced.append(", grunge style, distressed, vintage, artistic, textured, alternative aesthetic");
//                break;
//            case "cyberpunk":
//                enhanced.append(", cyberpunk style, neon lights, futuristic, digital art, dystopian atmosphere");
//                break;
//            case "impressionism":
//                enhanced.append(", impressionist style, soft brushstrokes, light and color, painterly texture");
//                break;
//            case "gothic":
//                enhanced.append(", gothic style, dark atmosphere, dramatic lighting, mysterious, ornate details");
//                break;
//            case "pop_art":
//                enhanced.append(", pop art style, bold colors, graphic design, contemporary art, vibrant");
//                break;
//            case "classical":
//                enhanced.append(", classical art style, renaissance inspired, traditional composition, elegant");
//                break;
//            case "abstract":
//                enhanced.append(", abstract art style, non-representational, geometric forms, creative expression");
//                break;
//            case "surrealism":
//                enhanced.append(", surrealist style, dreamlike, fantastical, imaginative, bizarre elements");
//                break;
//            case "modern":
//                enhanced.append(", modern art style, contemporary design, clean lines, minimalist aesthetic");
//                break;
//            case "sketch":
//                enhanced.append(", pencil sketch style, line art, hand-drawn, artistic sketch, monochrome");
//                break;
//            case "vintage":
//                enhanced.append(", vintage style, retro aesthetic, aged appearance, nostalgic atmosphere");
//                break;
//            default:
//                // Для пользовательских стилей добавляем базовые улучшения
//                enhanced.append(", ").append(style).append(" style, high quality, detailed artwork");
//                break;
//        }
//
//        return enhanced.toString();
//    }
//
//    private String enhancePromptForFlux(String prompt, String style) {
//        StringBuilder enhanced = new StringBuilder(prompt);
//
//        if (style == null) {
//            enhanced.append(", high quality digital art");
//            return enhanced.toString();
//        }
//
//        switch (style) {
//            case "realism":
//                enhanced.append(", hyperrealistic, 8k resolution, professional lighting, photographic quality");
//                break;
//            case "anime":
//                enhanced.append(", anime art style, detailed anime character design, cel-shaded, vibrant");
//                break;
//            case "artistic":
//                enhanced.append(", artistic masterpiece, creative composition, expressive art style");
//                break;
//            case "grunge":
//                enhanced.append(", grunge art style, rough textures, alternative aesthetic, distressed look");
//                break;
//            case "cyberpunk":
//                enhanced.append(", cyberpunk aesthetic, neon colors, futuristic technology, sci-fi atmosphere");
//                break;
//            case "impressionism":
//                enhanced.append(", impressionist painting style, soft brushwork, atmospheric lighting");
//                break;
//            case "gothic":
//                enhanced.append(", gothic art style, dark romantic, ornate architecture, dramatic shadows");
//                break;
//            case "pop_art":
//                enhanced.append(", pop art style, bold graphic design, bright colors, contemporary art");
//                break;
//            case "classical":
//                enhanced.append(", classical art style, renaissance painting, traditional techniques");
//                break;
//            case "abstract":
//                enhanced.append(", abstract expressionism, non-figurative art, geometric composition");
//                break;
//            case "surrealism":
//                enhanced.append(", surrealist art, dreamlike imagery, fantastical elements, imaginative");
//                break;
//            case "modern":
//                enhanced.append(", modern art style, contemporary design, clean aesthetic, geometric");
//                break;
//            case "sketch":
//                enhanced.append(", detailed sketch, line art, pencil drawing style, artistic illustration");
//                break;
//            case "vintage":
//                enhanced.append(", vintage art style, retro design, aged textures, nostalgic feel");
//                break;
//            default:
//                // Для пользовательских стилей
//                enhanced.append(", ").append(style).append(", high quality digital artwork, detailed");
//                break;
//        }
//
//        return enhanced.toString();
//    }
//
//    private String enhancePromptForPika(String prompt, String style) {
//        StringBuilder enhanced = new StringBuilder(prompt);
//        enhanced.append(", smooth animation, high quality video");
//
//        switch (style) {
//            case "anime":
//                enhanced.append(", anime animation style, 2D animation");
//                break;
//            case "realism":
//                enhanced.append(", realistic movement, natural lighting");
//                break;
//        }
//
//        return enhanced.toString();
//    }
//
//    private String enhancePromptForRunway(String prompt, String style) {
//        StringBuilder enhanced = new StringBuilder(prompt);
//        enhanced.append(", cinematic quality, smooth motion");
//
//        switch (style) {
//            case "realism":
//                enhanced.append(", realistic video, natural movement");
//                break;
//            case "cyberpunk":
//                enhanced.append(", cyberpunk atmosphere, futuristic setting");
//                break;
//        }
//
//        return enhanced.toString();
//    }
//
//    /**
//     * Получение endpoint для модели
//     */
//    private String getApiEndpoint(String model) {
//        return switch (model) {
//            case "dall-e-3" -> "/v1/images/generations";
//            case "flux" -> "/v1/flux/generations";
//            case "pika" -> "/v1/pika/generations";
//            case "runway" -> "/v1/runway/generations";
//            default -> throw new IllegalArgumentException("Неизвестная модель: " + model);
//        };
//    }
//
//    /**
//     * Извлечение URL результата из ответа API
//     */
//    private String extractResultUrl(Map<String, Object> response, GenerationRequestDTO dto) {
//        try {
//            // Для DALL-E 3
//            if ("dall-e-3".equals(dto.getModel()) && response.containsKey("data")) {
//                @SuppressWarnings("unchecked")
//                java.util.List<Map<String, Object>> dataList =
//                        (java.util.List<Map<String, Object>>) response.get("data");
//                if (!dataList.isEmpty()) {
//                    return (String) dataList.get(0).get("url");
//                }
//            }
//
//            // Для Flux
//            if ("flux".equals(dto.getModel()) && response.containsKey("url")) {
//                return (String) response.get("url");
//            }
//
//            // Для видео (Pika, Runway)
//            if ((dto.getMediaType() == com.server.telegramservice.entity.enums.MediaType.VIDEO)) {
//                if (response.containsKey("video_url")) {
//                    return (String) response.get("video_url");
//                }
//                if (response.containsKey("url")) {
//                    return (String) response.get("url");
//                }
//            }
//
//            // Общий случай
//            if (response.containsKey("url")) {
//                return (String) response.get("url");
//            }
//
//            log.warn("⚠️ Не удалось извлечь URL из ответа: {}", response);
//            return null;
//
//        } catch (Exception e) {
//            log.error("❌ Ошибка извлечения URL: {}", e.getMessage());
//            return null;
//        }
//    }
//
//    /**
//     * Загрузка результата в S3
//     */
//    private String uploadResultToS3(String resultUrl, GenerationRequestDTO dto, String operationId) {
//        try {
//            log.debug("📦 [{}] Загружаем в S3: {}", operationId, resultUrl);
//
//            // Скачиваем файл
//            ResponseEntity<byte[]> response = restTemplate.getForEntity(resultUrl, byte[].class);
//
//            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
//                byte[] fileData = response.getBody();
//
//                // Определяем параметры файла
//                String folder = dto.getMediaType() == com.server.telegramservice.entity.enums.MediaType.IMAGE ?
//                        "images" : "videos";
//                String extension = dto.getMediaType() == com.server.telegramservice.entity.enums.MediaType.IMAGE ?
//                        ".png" : ".mp4";
//                String filename = String.format("%s_%s_%s%s",
//                        operationId, dto.getModel(), dto.getStyle(), extension);
//                String contentType = dto.getMediaType() == com.server.telegramservice.entity.enums.MediaType.IMAGE ?
//                        "image/png" : "video/mp4";
//
//                // Загружаем в S3
//                String s3Url = s3Service.uploadBytes(fileData, folder, filename, contentType);
//
//                log.info("✅ [{}] Файл загружен в S3: {}", operationId, s3Url);
//                return s3Url;
//
//            } else {
//                throw new RuntimeException("Не удалось скачать файл: " + response.getStatusCode());
//            }
//
//        } catch (Exception e) {
//            log.error("❌ [{}] Ошибка загрузки в S3: {}", operationId, e.getMessage(), e);
//            throw new RuntimeException("Ошибка загрузки в S3", e);
//        }
//    }
//
//    // Утилитарные методы для обновления БД
//
//    private void updateStatus(GenerationRequest entity, GenerationStatus status) {
//        entity.setStatus(status);
//        generationRequestRepository.save(entity);
//    }
//
//    private void updateSuccessfulGeneration(GenerationRequest entity, String resultUrl, String s3Url, long startTime) {
//        entity.setStatus(GenerationStatus.SUCCEEDED);
//        entity.setResultUrl(resultUrl);
//        entity.setS3Url(s3Url);
//        entity.setCompletedAt(LocalDateTime.now());
//        entity.setProcessingTimeMs(System.currentTimeMillis() - startTime);
//        generationRequestRepository.save(entity);
//    }
//
//    private void updateFailedGeneration(GenerationRequest entity, String errorMessage, long startTime) {
//        entity.setStatus(GenerationStatus.FAILED);
//        entity.setErrorMessage(errorMessage);
//        entity.setCompletedAt(LocalDateTime.now());
//        entity.setProcessingTimeMs(System.currentTimeMillis() - startTime);
//        generationRequestRepository.save(entity);
//    }
//
//    // Утилитарные методы конвертации
//
//    private String convertAspectRatioToDallESize(String aspectRatio) {
//        return switch (aspectRatio) {
//            case "1:1" -> "1024x1024";
//            case "16:9" -> "1792x1024";
//            case "9:16" -> "1024x1792";
//            default -> "1024x1024";
//        };
//    }
//
//    private String convertStyleForPika(String style) {
//        return switch (style) {
//            case "anime" -> "anime";
//            case "realism" -> "realistic";
//            case "cyberpunk" -> "cyberpunk";
//            case "grunge" -> "artistic";
//            default -> "realistic";
//        };
//    }
//}
