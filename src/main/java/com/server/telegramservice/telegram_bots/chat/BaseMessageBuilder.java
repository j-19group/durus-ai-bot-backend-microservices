package com.server.telegramservice.telegram_bots.chat;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.Arrays;
import java.util.List;

public abstract class BaseMessageBuilder {

    public static InlineKeyboardMarkup createBaseFormatKeyboard() {
        List<List<InlineKeyboardButton>> rows = Arrays.asList(
                Arrays.asList(createInlineButton("⬜ Квадрат (1:1)", "format_square")),
                Arrays.asList(createInlineButton("📺 Широкий (16:9)", "format_wide")),
                Arrays.asList(createInlineButton("📱 Вертикальный (9:16)", "format_vertical"))
        );
        return new InlineKeyboardMarkup(rows);
    }

    protected static InlineKeyboardButton createInlineButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }
}
