package com.server.telegramservice.telegram_bots.chat.impl;

import com.server.telegramservice.telegram_bots.chat.BaseMessageBuilder;
import com.server.telegramservice.telegram_bots.chat.BotMessageBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.Arrays;
import java.util.List;

@Service("photoBotMessageBuilder")
@RequiredArgsConstructor
public class PhotoBotMessageBuilder implements BotMessageBuilder {

    @Override
    public String getBotDisplayName() {
        return "DURUS-AI PhotoBot / ФотоКарточка";
    }

    @Override
    public String getBotCapabilities() {
        return
            """
            Я создаю изображения в любом стиле:
            📸 От фотореализма до аниме
            🎨 От классики до современного искусства
            ✨ Или в вашем уникальном стиле!
            """;
    }

    @Override
    public String getHelpContent() {
        return
            """
            📖 Помощь по PhotoBot:
            ... [фото-специфичная помощь] ...
            """;
    }

    @Override
    public InlineKeyboardMarkup createStyleKeyboard() {

        List<List<InlineKeyboardButton>> rows = Arrays.asList(
                Arrays.asList(
                        createInlineButton("📸 Реализм", "style_realism"),
                        createInlineButton("🌸 Аниме", "style_anime")
                ),
                Arrays.asList(
                        createInlineButton("🎨 Художественный", "style_artistic"),
                        createInlineButton("🔥 Гранж", "style_grunge")
                ),
                Arrays.asList(
                        createInlineButton("🚀 Киберпанк", "style_cyberpunk"),
                        createInlineButton("🌅 Импрессионизм", "style_impressionism")
                ),
                Arrays.asList(
                        createInlineButton("🖤 Готика", "style_gothic"),
                        createInlineButton("🌈 Поп-арт", "style_pop_art")
                ),
                Arrays.asList(
                        createInlineButton("🏛️ Классический", "style_classical"),
                        createInlineButton("🌊 Абстракция", "style_abstract")
                ),
                Arrays.asList(
                        createInlineButton("🎭 Сюрреализм", "style_surrealism"),
                        createInlineButton("🏙️ Модерн", "style_modern")
                ),
                Arrays.asList(
                        createInlineButton("✏️ Эскиз", "style_sketch"),
                        createInlineButton("🖼️ Винтаж", "style_vintage")
                ),
                Arrays.asList(
                        createInlineButton("✨ Свой стиль", "style_custom")
                )
        );
        return new InlineKeyboardMarkup(rows);
    }
    private InlineKeyboardButton createStyleButton(String text, String style) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData("style_" + style);
        return button;
    }

    public InlineKeyboardButton createInlineButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }
    @Override
    public InlineKeyboardMarkup createFormatKeyboard() {
        // Общая клавиатура форматов
        return BaseMessageBuilder.createBaseFormatKeyboard();
    }
}