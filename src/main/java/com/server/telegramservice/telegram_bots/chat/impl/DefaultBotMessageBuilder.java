package com.server.telegramservice.telegram_bots.chat.impl;

import com.server.telegramservice.telegram_bots.chat.BaseMessageBuilder;
import com.server.telegramservice.telegram_bots.chat.BotMessageBuilder;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Service
public class DefaultBotMessageBuilder implements BotMessageBuilder {
    @Override
    public String getBotDisplayName() {
        return "DURUS-AI Bot";
    }

    @Override
    public String getBotCapabilities() {
        return "Универсальный AI помощник!";
    }

    @Override
    public String getHelpContent() {
        return "Базовая помощь по боту.";
    }

    @Override
    public InlineKeyboardMarkup createStyleKeyboard() {
        // Базовая клавиатура стилей
        return new InlineKeyboardMarkup();
    }

    @Override
    public InlineKeyboardMarkup createFormatKeyboard() {
        return BaseMessageBuilder.createBaseFormatKeyboard();
    }
}
