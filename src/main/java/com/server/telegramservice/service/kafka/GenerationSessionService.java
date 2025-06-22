package com.server.telegramservice.service.generation;

import com.server.telegramservice.dto.requests.GenerationRequestDTO;
import com.server.telegramservice.dto.requests.GenerationRequest;
import com.server.telegramservice.entity.enums.*;
import com.server.telegramservice.entity.telegram.ChatSession;
import com.server.telegramservice.entity.telegram.User;
import com.server.telegramservice.entity.repository.GenerationRequestRepository;
import com.server.telegramservice.telegram_bots.chat.impl.ChatPersistenceService;
import com.server.telegramservice.service.kafka.GenerationEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenerationSessionService {

    List<GenerationStatus> completedStatuses = List.of(
            GenerationStatus.SUCCEEDED,
            GenerationStatus.FAILED,
            GenerationStatus.CANCELED,
            GenerationStatus.TIMEOUT,
            GenerationStatus.UNKNOWN
    );
    private final ChatPersistenceService chatPersistenceService;
    private final GenerationRequestRepository generationRequestRepository;
    private final GenerationEventProducer eventProducer;
    private final RedisTemplate<String, Object> redisTemplate;

    // Кэш активных сессий генерации
    private final Map<String, GenerationRequestDTO> activeGenerations = new ConcurrentHashMap<>();

    private static final String REDIS_PREFIX = "generation_session:";
    private static final Duration SESSION_TTL = Duration.ofHours(2);

    /**
     * Проверяет, может ли пользователь создать новый запрос
     */
    public boolean canUserCreateNewGeneration(Long userId) {
        // Проверяем активные запросы в БД
        Optional<GenerationRequest> activeRequest =
                generationRequestRepository.findTopByUserTelegramIdAndStatusNotInOrderByCreatedAtDesc(
                        userId, completedStatuses
                );

        if (activeRequest.isPresent()) {
            log.info("🚫 Пользователь {} имеет активный запрос: {}",
                    userId, activeRequest.get().getOperationId());
            return false;
        }

        return true;
    }

    /**
     * Получает активную сессию генерации для пользователя
     */
    public Optional<GenerationRequestDTO> getActiveGenerationSession(String userId) {
        // Сначала проверяем в памяти
        GenerationRequestDTO session = activeGenerations.get(userId);
        if (session != null) {
            return Optional.of(session);
        }

        // Проверяем в Redis
        String redisKey = REDIS_PREFIX + userId;
        session = (GenerationRequestDTO) redisTemplate.opsForValue().get(redisKey);

        if (session != null) {
            activeGenerations.put(userId, session);
            return Optional.of(session);
        }

        return Optional.empty();
    }

    /**
     * Создает новую сессию генерации
     */
    public GenerationRequestDTO createGenerationSession(Long userId, Long chatId, String prompt,
                                                        MediaType mediaType, Bot botType) {
        String userIdStr = String.valueOf(userId);

        // Проверяем, может ли пользователь создать новый запрос
        if (!canUserCreateNewGeneration(userId)) {
            throw new IllegalStateException("У пользователя уже есть активный запрос генерации");
        }

        String operationId = UUID.randomUUID().toString();

        GenerationRequestDTO dto = new GenerationRequestDTO();
        dto.setOperationId(operationId);
        dto.setUserId(userId);
        dto.setChatId(chatId);
        dto.setPrompt(prompt);
        dto.setMediaType(mediaType);
        dto.setBot(botType);
        dto.setStatus(GenerationStatus.PENDING);
        dto.setCreatedAt(LocalDateTime.now());

        // Сохраняем в память и Redis
        activeGenerations.put(userIdStr, dto);
        String redisKey = REDIS_PREFIX + userIdStr;
        redisTemplate.opsForValue().set(redisKey, dto, SESSION_TTL);

        log.info("🆕 Создана сессия генерации [{}] для пользователя {}", operationId, userId);
        return dto;
    }

    /**
     * Обновляет сессию генерации
     */
    public void updateGenerationSession(String userId, GenerationRequestDTO dto) {
        activeGenerations.put(userId, dto);
        String redisKey = REDIS_PREFIX + userId;
        redisTemplate.opsForValue().set(redisKey, dto, SESSION_TTL);

        log.debug("🔄 Обновлена сессия генерации [{}] для пользователя {}",
                dto.getOperationId(), userId);
    }

    /**
     * Отправляет запрос на генерацию в Kafka и сохраняет в БД
     */
    public void submitGenerationRequest(GenerationRequestDTO dto) {
        String userId = String.valueOf(dto.getUserId());

        try {
            // Сохраняем в БД
            GenerationRequest entity = saveGenerationRequestToDb(dto);

            // Отправляем в Kafka
            eventProducer.sendGenerationRequest(dto.getOperationId(), dto);

            // Обновляем статус
            dto.setStatus(GenerationStatus.PENDING);
            updateGenerationSession(userId, dto);

            log.info("📨 Запрос генерации [{}] отправлен в обработку", dto.getOperationId());

        } catch (Exception e) {
            log.error("❌ Ошибка отправки запроса генерации [{}]: {}",
                    dto.getOperationId(), e.getMessage());
            throw new RuntimeException("Ошибка отправки запроса на генерацию", e);
        }
    }

    /**
     * Завершает сессию генерации
     */
    public void completeGenerationSession(String userId, String operationId,
                                          GenerationStatus status, String resultUrl) {
        // Удаляем из активных сессий
        activeGenerations.remove(userId);
        String redisKey = REDIS_PREFIX + userId;
        redisTemplate.delete(redisKey);

        log.info("✅ Завершена сессия генерации [{}] для пользователя {} со статусом {}",
                operationId, userId, status);
    }

    /**
     * Получает статус генерации по operationId
     */
    public Optional<GenerationRequest> getGenerationStatus(String operationId) {
        return generationRequestRepository.findByOperationId(operationId);
    }

    // Приватные методы

    private GenerationRequest saveGenerationRequestToDb(GenerationRequestDTO dto) {
        // Получаем пользователя и сессию
        User user = chatPersistenceService.getOrCreateUser(dto.getUserId(), null);
        ChatSession session = chatPersistenceService.getOrCreateActiveSession(user);

        GenerationRequest entity = new GenerationRequest();
        entity.setOperationId(dto.getOperationId());
        entity.setUser(user);
        entity.setSession(session);
        entity.setChatId(dto.getChatId());
        entity.setPrompt(dto.getPrompt());
        entity.setMediaType(dto.getMediaType());
        entity.setStyle(dto.getStyle());
        entity.setAspectRatio(dto.getAspectRatio());
        entity.setModel(dto.getModel());
        entity.setStatus(dto.getStatus());
        entity.setNegativePrompt(dto.getNegativePrompt());
        entity.setSeed(dto.getSeed());
        entity.setStrength(dto.getStrength());
        entity.setSteps(dto.getSteps());
        entity.setQuality(dto.getQuality());

        return generationRequestRepository.save(entity);
    }
}
