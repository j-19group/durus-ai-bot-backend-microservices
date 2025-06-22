package com.server.telegramservice.telegram_bots;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractTelegramBot extends TelegramLongPollingBot {

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                handleMessage(update.getMessage());
            } else if (update.hasCallbackQuery()) {
                handleCallbackQuery(update.getCallbackQuery());
            }
        } catch (Exception e) {
            log.error("❌ [{}} Ошибка обработки update: {}", getBotUsername(), e.getMessage(), e);
            handleError(update, e);
        }
    }

    protected abstract void handleMessage(Message message);

    protected abstract void handleCallbackQuery(CallbackQuery callbackQuery);

    protected void handleError(Update update, Exception e) {
        Long chatId = null;
        if (update.hasMessage()) {
            chatId = update.getMessage().getChatId();
        } else if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
        }

        if (chatId != null) {
            // Отправляем сообщение об ошибке через конкретную имплементацию
            sendErrorMessage(chatId, "Произошла ошибка. Попробуйте еще раз.");
        }
    }

    protected abstract void sendErrorMessage(Long chatId, String errorText);
}