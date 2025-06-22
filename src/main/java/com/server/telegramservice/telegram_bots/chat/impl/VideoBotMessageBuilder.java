package com.server.telegramservice.telegram_bots.chat.impl;

import com.server.telegramservice.telegram_bots.chat.BaseMessageBuilder;
import com.server.telegramservice.telegram_bots.chat.BotMessageBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoBotMessageBuilder implements BotMessageBuilder {

    @Override
    public String getBotDisplayName() {
        return "DURUS-AI VideoBot / ВидеоКарточка";
    }

    @Override
    public String getBotCapabilities() {
        return """
            Я создаю видео в разных стилях:
            🎬 Анимационные ролики
            🎥 Реалистичные сцены
            ✨ Креативные видео-арт проекты!
            """;
    }

    @Override
    public String getHelpContent() {
        return """
            📖 Помощь по VideoBot:
            ... [видео-специфичная помощь] ...
            """;
    }

    @Override
    public InlineKeyboardMarkup createStyleKeyboard() {
        // Клавиатура стилей для видео
        List<List<InlineKeyboardButton>> rows = Arrays.asList(
                // ... видео-специфичные стили ...
        );
        return new InlineKeyboardMarkup(rows);
    }

    @Override
    public InlineKeyboardMarkup createFormatKeyboard() {
        // Общая клавиатура форматов
        return BaseMessageBuilder.createBaseFormatKeyboard();
    }
}
