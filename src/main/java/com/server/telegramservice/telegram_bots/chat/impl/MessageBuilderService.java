package com.server.telegramservice.telegram_bots.chat.impl;

import com.server.telegramservice.entity.enums.Bot;
import com.server.telegramservice.entity.enums.GenerationStatus;
import com.server.telegramservice.entity.telegram.User;
import com.server.telegramservice.telegram_bots.chat.BaseMessageBuilder;
import com.server.telegramservice.telegram_bots.chat.BotMessageBuilder;
import com.server.telegramservice.telegram_bots.AbstractTelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageBuilderService {
    private final Map<Bot, BotMessageBuilder> messageBuilders;

    @Autowired
    public MessageBuilderService(List<BotMessageBuilder> builders) {
        this.messageBuilders = builders.stream()
                .collect(Collectors.toMap(
                        builder -> Bot.valueOf(builder.getClass().getSimpleName()
                                .replace("MessageBuilder", "")
                                .toUpperCase()),
                        Function.identity()
                ));
    }

    private BotMessageBuilder getBuilder(Bot botType) {
        return messageBuilders.getOrDefault(botType, new DefaultBotMessageBuilder());
    }

    public void sendWelcomeMessage(AbstractTelegramBot bot, Long chatId, User user, Bot botType) {
        BotMessageBuilder builder = getBuilder(botType);
        String welcomeMessage = String.format(
                "🎨 Добро пожаловать в %s, %s!\n\n%s\n\nПросто опишите, что хотите создать!",
                builder.getBotDisplayName(),
                user.getUsername() != null ? user.getUsername() : "друг",
                builder.getBotCapabilities()
        );
        sendTextMessage(bot, chatId, welcomeMessage);
    }

    public void sendStyleSelection(AbstractTelegramBot bot, Long chatId, Bot botType) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🎭 Выберите стиль или введите свой:");
        message.setReplyMarkup(getBuilder(botType).createStyleKeyboard());
        executeMessage(bot, message);
    }

    public void sendFormatSelection(AbstractTelegramBot bot, Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("📐 Выберите формат:");
        message.setReplyMarkup(BaseMessageBuilder.createBaseFormatKeyboard());
        executeMessage(bot, message);
    }
    public void sendTextMessage(AbstractTelegramBot bot, Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            log.error("❌ Ошибка отправки сообщения: {}", e.getMessage());
        }
      
    }
    private String getStatusEmoji(GenerationStatus status) {
        return switch (status) {
            case PENDING -> "⏳";
            case RUNNING -> "🔄";
            case SUCCEEDED -> "✅";
            case FAILED -> "❌";
            case CREATED, CANCELED, TIMEOUT, UNKNOWN -> null;
        };
    }
    private void executeMessage(AbstractTelegramBot bot, SendMessage message) {
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            log.error("❌ Ошибка отправки сообщения: {}", e.getMessage(), e);
        }
    }

}

