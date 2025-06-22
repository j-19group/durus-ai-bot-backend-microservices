package com.server.telegramservice.telegram_bots;

import com.server.telegramservice.entity.enums.Bot;
import com.server.telegramservice.entity.enums.MessageType;
import com.server.telegramservice.entity.telegram.ChatSession;
import com.server.telegramservice.entity.enums.Sender;
import com.server.telegramservice.entity.telegram.User;
import com.server.telegramservice.telegram_bots.chat.impl.ChatPersistenceService;
import com.server.telegramservice.service.tgchat.CommandHandlerService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TargetingBot extends TelegramLongPollingBot {

    private final OpenAiService openAiService;
    private final CommandHandlerService commandHandler;
    private final ChatPersistenceService chatPersistenceService;

    @Value("${telegrambots.bots[0].username}")
    private String botUsername;

    @Value("${telegrambots.bots[0].token}")
    private String botToken;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String userInput = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            Long telegramId = update.getMessage().getFrom().getId();
            String username = update.getMessage().getFrom().getUserName();

            // Сначала создаём/находим пользователя
            User user = chatPersistenceService.getOrCreateUser(telegramId, username);

            String reply;

            if (userInput.startsWith("/")) {
                // Команда
                reply = commandHandler.handleCommand(userInput, username);

                // Обработка команды /newsession — закрываем текущую сессию
                if (userInput.equalsIgnoreCase("/newsession")) {
                    chatPersistenceService.endActiveSession(user);
                }

            } else {
                // Вопрос → GPT
                ChatSession session = chatPersistenceService.getOrCreateActiveSession(user);

                // сейвим сообщение
                chatPersistenceService.saveMessage(session, Sender.USER, userInput, Bot.TARGETOLOG, MessageType.TELEGRAM);

                // Получаем ответ от GPT
                reply = askAsMarketingExpert(userInput);

                // cейвим ответ
                chatPersistenceService.saveMessage(session, Sender.BOT, reply, Bot.TARGETOLOG, MessageType.TELEGRAM);
            }

            // Отправка сообщения в Telegram
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText(reply);

            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private String askAsMarketingExpert(String prompt) {
        ChatMessage systemPrompt = new ChatMessage(ChatMessageRole.SYSTEM.value(),
                "Ты опытный таргетолог с более чем 10 годами работы в digital-маркетинге. " +
                        "Специализируешься на Facebook Ads, Google Ads, TikTok Ads и других площадках. " +

                        "ТВОИ КЛЮЧЕВЫЕ НАВЫКИ: " +
                        "• Создание высококонверсионных креативов и текстов " +
                        "• Настройка точного таргетинга и аудиторий " +
                        "• A/B тестирование и оптимизация кампаний " +
                        "• Анализ метрик (CTR, CPC, ROAS, LTV) " +
                        "• Работа с воронками продаж и ретаргетингом " +

                        "АЛГОРИТМ НАСТРОЙКИ РЕКЛАМЫ: " +
                        "1. Исследование продукта и целевой аудитории " +
                        "2. Подбор площадок и форматов " +
                        "3. Разработка креативов и подготовка посадочных страниц " +
                        "4. Настройка кампаний и распределение бюджета " +
                        "5. Запуск, анализ метрик и оптимизация " +

                        "ИСПОЛЬЗУЙ ПСИХОЛОГИЧЕСКИЕ ТРИГГЕРЫ: " +
                        "• FOMO (страх упустить выгоду) " +
                        "• Социальное доказательство " +
                        "• Ограниченность предложения " +
                        "• Эффект якорения в цене " +
                        "• Когнитивные искажения " +
                        "• Сторителлинг и эмоции " +

                        "ФОРМАТ ОТВЕТОВ: " +
                        "• Чёткие рекомендации с цифрами " +
                        "• Примеры креативов и заголовков " +
                        "• Список метрик для контроля " +
                        "• Рекомендации по бюджету и срокам " +

                        "ПРАВИЛА ОТВЕТОВ ДЛЯ ТЕЛЕГРАМА: " +
                        "• Не используй символы # и ** " +
                        "• Применяй разметку Telegram: *жирный*, _курсив_, `код`, ```блок кода``` " +
                        "• Структурируй текст знаками —, •, 1., 2. " +
                        "• Фокусируйся только на продукте клиента " +
                        "• Везде указывай цифры и примеры " +
                        "• Валюта по умолчанию: тенге (KZT) " +
                        "• Пиши кратко, удобно для чтения с телефона " +
                        "• Добавляй эмодзи для акцентов: 🎯 📊 💰 ⚡ " +

                        "ВАЖНО: ты только таргетолог и не выходишь за рамки этой роли.");
        ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(), prompt);

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .messages(List.of(systemPrompt, userMessage))
                .temperature(0.7)
                .maxTokens(500)
                .build();

        try {
            ChatCompletionResult result = openAiService.createChatCompletion(request);
            String response = result.getChoices().get(0).getMessage().getContent();
            return response;

        } catch (Exception e) {
            log.error("Ошибка при обращении к OpenAI: " + e.getMessage());
            return "Ошибка сервера";
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}

