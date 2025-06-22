package com.server.telegramservice.entity.enums;

public enum GenerationStatus {
    CREATED,        // Задача создана
    PENDING,        // Задача в очереди
    RUNNING,        // Задача выполняется
    SUCCEEDED,      // Успешно завершено
    FAILED,         // Ошибка при выполнении
    CANCELED,       // Отменено пользователем или системой
    TIMEOUT,        // Превышено время ожидания
    UNKNOWN         // Неизвестный статус / дефолт
}
