package com.server.telegramservice.telegram_bots.chat;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

public interface BotMessageBuilder {
    String getBotDisplayName();
    String getBotCapabilities();
    String getHelpContent();
    InlineKeyboardMarkup createStyleKeyboard();
    InlineKeyboardMarkup createFormatKeyboard();
    InlineKeyboardButton createInlineButton(String text, String callbackData);
}