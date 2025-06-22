package com.server.telegramservice.telegram_bots;

import com.server.telegramservice.entity.enums.*;
import com.server.telegramservice.entity.telegram.ChatSession;
import com.server.telegramservice.entity.telegram.User;
import com.server.telegramservice.service.ai.chat.ChatService;
import com.server.telegramservice.telegram_bots.chat.impl.ChatPersistenceService;
import com.server.telegramservice.dto.requests.*;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import com.server.telegramservice.telegram_bots.chat.BotMessageBuilder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class PhotoBot extends AbstractTelegramBot {

    private final OpenAiService openAiService;
    @Value("${telegrambots.bots[2].username}")
    private String botUsername;

    @Value("${telegrambots.bots[2].token}")
    private String botToken;

    @Qualifier("openAiChatService")
    private final ChatService chatService;

    @Qualifier("photoBotMessageBuilder")
    private final BotMessageBuilder botMessageBuilder;

    private final ChatPersistenceService chatPersistenceService;

    // Кэш для хранения временных данных генерации
    private final Map<String, GenerationRequestDTO> userGenerationCache = new ConcurrentHashMap<>();

    // Системные промпты
    private static final String SYSTEM_PROMPT_ANALYSIS = """
        Ты - помощник по анализу запросов для генерации изображений и видео.
        Проанализируй запрос пользователя и определи:
        1. Достаточно ли информации для генерации
        2. Какой тип контента нужен (фото/видео)
        3. Какие параметры отсутствуют
        
        Отвечай кратко и по делу. Если нужны уточнения - задай конкретные вопросы.
        Если пользователь просит что то создать то и тебе не нужны уточнение то напиши только CREATE; 
        """;

    private static final String SYSTEM_PROMPT_CHAT = """
        Ты - дружелюбный AI-помощник бота для создания изображений и видео.
        Помогай пользователям с текстовыми вопросами, объясняй возможности бота.
        Отвечай коротко и полезно.
        """;


    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                handleMessage(update.getMessage());
            } else if (update.hasCallbackQuery()) {
                handleCallbackQuery(update.getCallbackQuery());
            }
        } catch (Exception e) {
            log.error("❌ Ошибка обработки update: {}", e.getMessage(), e);

            Long chatId = null;
            if (update.hasMessage()) {
                chatId = update.getMessage().getChatId();
            } else if (update.hasCallbackQuery()) {
                chatId = update.getCallbackQuery().getMessage().getChatId();
            }

            if (chatId != null) {
                sendErrorMessage(chatId, "Произошла ошибка. Попробуйте еще раз.");
            }
        }
    }

    @Override
    protected void handleMessage(Message message) {
        Long chatId = message.getChatId();
        String messageText = message.getText();

        log.info("📨 Получено сообщение от {}: {}", chatId, messageText);

        // Получаем или создаем пользователя и сессию
        User user = getOrCreateUser(message);
        ChatSession session = chatPersistenceService.getOrCreateActiveSession(user);

        // Сохраняем сообщение пользователя
        chatPersistenceService.saveMessage(session, Sender.USER, messageText, Bot.PHOTO_BOT, MessageType.TELEGRAM);

        if (messageText.equals("/start")) {
            handleStartCommand(chatId, user);
        } else if (messageText.equals("/help")) {
            handleHelpCommand(chatId);
        } else if (messageText.equals("/new")) {
            handleNewCommand(chatId, user);
        } else if (isMediaGenerationRequest(messageText)) {
            handleMediaGenerationRequest(chatId, messageText, session);
        } else {
            handleTextChat(chatId, messageText, session);
        }
    }


    protected void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        String userId = String.valueOf(callbackQuery.getFrom().getId());

        log.info("🔘 Callback от {}: {}", chatId, callbackData);

        try {
            if (callbackData.startsWith("style_")) {
                handleStyleSelection(chatId, userId, callbackData);
            } else if (callbackData.startsWith("format_")) {
                handleFormatSelection(chatId, userId, callbackData);

            } else if (callbackData.equals("confirm_generation")) {
             handleGenerationConfirmation(chatId, userId);
            } else if (callbackData.equals("edit_parameters")) {
                handleParameterEdit(chatId, userId);
            } else if (callbackData.startsWith("rate_")) {
                handleRating(chatId, callbackData);
            }

            // Удаляем inline keyboard
            org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup editMarkup =
                    new org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup();
            editMarkup.setChatId(chatId);
            editMarkup.setMessageId(callbackQuery.getMessage().getMessageId());
            editMarkup.setReplyMarkup(null);
            execute(editMarkup);

        } catch (TelegramApiException e) {
            log.error("❌ Ошибка обработки callback: {}", e.getMessage());
        }
    }

    private void handleGenerationConfirmation(Long chatId, String userId) {
        if isMediaGenerationRequest()
          handleMediaGenerationRequest
                  тут нужно проверить все ещ ераз
    }

    private void handleStartCommand(Long chatId, User user) {
        String botName = botMessageBuilder.getBotDisplayName();
        String capabilities = botMessageBuilder.getBotCapabilities();

        String welcomeMessage = String.format("""
            🎨 Добро пожаловать в %s, %s!
            
            %s
            
            Просто опишите, что хотите увидеть!
            
            🌟 Примеры:
            • "Кот-астронавт в космосе"
            • "Девушка в стиле аниме с голубыми волосами"
            • "Городской пейзаж в стиле импрессионизм"
            • "Портрет в готическом стиле"
            
            📋 Команды:
            /help - подробная помощь
            /new - новый проект
            """,
                botName,
                user.getUsername() != null ? user.getUsername() : "друг",
                capabilities
        );

        sendTextMessage(chatId, welcomeMessage);
    }

    private void handleHelpCommand(Long chatId) {
        String helpMessage = botMessageBuilder.getHelpContent();
        sendTextMessage(chatId, helpMessage);
    }

    private void handleNewCommand(Long chatId, User user) {
        // Очищаем кэш для пользователя
        String userId = String.valueOf(user.getTelegramId());
        userGenerationCache.remove(userId);

        // Завершаем текущую сессию и создаем новую
        chatPersistenceService.endActiveSession(user);
        ChatSession newSession = chatPersistenceService.getOrCreateActiveSession(user);

        sendTextMessage(chatId, "🆕 Начинаем новый проект! Опишите, что хотите создать.");
    }

    private boolean isMediaGenerationRequest(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        final String CLASSIFICATION_PROMPT = """
        Ты - классификатор запросов. Определи, является ли запрос пользователя 
        запросом на генерацию изображения или видео. Отвечай только "да" или "нет".
        
        Примеры запросов на генерацию:
        - "Создай изображение кота в скафандре"
        - "Нарисуй пейзаж заката в горах"
        - "Сгенерируй фото девушки в стиле аниме"
        - "Видео с летающими китами в космосе"
        - "Картинка абстрактного искусства"
        
        Примеры НЕ запросов на генерацию:
        - "Привет"
        - "Как тебя зовут?"
        - "Расскажи о возможностях"
        - "Что ты умеешь?"
        - "Помоги мне с идеей"
        """;

        try {
            // Отправляем запрос в модель
            ChatCompletionResult result = chatService.generateResponse(
                    "Запрос: " + text + "\n\nЭто запрос на генерацию изображения?",
                    CLASSIFICATION_PROMPT
            );

            String response = result.getChoices().get(0).getMessage().getContent().trim().toLowerCase();

            // Парсим ответ
            boolean isMediaRequest = response.contains("да") ||
                    response.contains("yes") ||
                    response.contains("1");



            return isMediaRequest;

        } catch (Exception e) {
            log.error("❌ Ошибка классификации запроса: {}", e.getMessage());
            throw e;
        }
    }

    private void handleMediaGenerationRequest(Long chatId, String prompt, ChatSession session) {
        String userId = String.valueOf(session.getUser().getTelegramId());

        try {
            // Анализируем запрос на достаточность информации
            final String ANALYSIS_PROMPT = """
            Ты - помощник по анализу запросов для генерации изображений.
            Проанализируй запрос пользователя и определи, достаточно ли информации для генерации.
            Ответь только "да" или "нет".
            
            Критерии достаточности:
            - Есть основной объект/субъект
            - Указан стиль или контекст
            - Присутствуют ключевые детали
            """;

            ChatCompletionResult analysis = chatService.generateResponseWithContext(
                    "Запрос: " + prompt,
                    ANALYSIS_PROMPT,session
            );

            String analysisResult = analysis.getChoices().get(0).getMessage().getContent().toLowerCase().trim();
            boolean isSufficient = analysisResult.contains("да");



            GenerationRequestDTO dto = new GenerationRequestDTO();
            dto.setPrompt(prompt);
            dto.setUserId(Long.parseLong(userId));
            dto.setChatId(chatId);
            dto.setMediaType(MediaType.IMAGE);

            if (isSufficient) {
                showGenerationPreview(chatId, dto);
            } else {
                // Запрашиваем дополнительные детали
                requestAdditionalDetails(chatId, prompt, dto);
            }

        } catch (Exception e) {
            log.error("❌ Ошибка анализа запроса: {}", e.getMessage());
            sendErrorMessage(chatId, "Не удалось проанализировать запрос. Попробуйте переформулировать.");
        }
    }

    private void requestAdditionalDetails(Long chatId, String originalPrompt, GenerationRequestDTO dto) {
        try {
            // Используем ChatGPT для генерации уточняющих вопросов
            final String DETAILS_PROMPT = """
            Ты - помощник для генерации изображений. 
            Пользователь предоставил недостаточно деталей в запросе: 
            "%s"
            
            Сгенерируй 1-2 конкретных вопроса, которые помогут уточнить:
            1. Стиль изображения
            2. Детали объекта
            3. Контекст или окружение
            4. Цветовую гамму
            
            Отвечай только самими вопросами без дополнительных объяснений.
            """;

            ChatCompletionResult detailsResponse = chatService.generateResponse(
                    String.format(DETAILS_PROMPT, originalPrompt),
                    ""
            );

            String questions = detailsResponse.getChoices().get(0).getMessage().getContent();


            // Отправляем вопросы пользователю
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("🖌️ Для лучшего результата уточните детали:\n\n" + questions);

            // Добавляем кнопку отмены
            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            keyboard.setKeyboard(Collections.singletonList(
                    Collections.singletonList(createInlineButton("❌ Отменить", "cancel_generation"))
            ));
            message.setReplyMarkup(keyboard);

            execute(message);

        } catch (Exception e) {
            log.error("❌ Ошибка генерации уточнений: {}", e.getMessage());
            sendTextMessage(chatId, "🖌️ Пожалуйста, уточните детали вашего запроса (стиль, цвета, детали объекта)");
        }
    }


    private void askForClarification(Long chatId, GenerationRequestDTO dto, String analysisResult) {
        if (dto.getStyle() == null) {
            askForStyle(chatId);
        } else if (dto.getAspectRatio() == null) {
            askForFormat(chatId);
        } else {
            // Если все основные параметры есть, показываем превью
            autoSelectModel(dto);
            showGenerationPreview(chatId, dto);
        }
    }

    private void askForStyle(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🎭 Выберите стиль или введите свой:");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = Arrays.asList(
                Arrays.asList(
                        botMessageBuilder.createInlineButton("📸 Реализм", "style_realism"),
                        botMessageBuilder.createInlineButton("🌸 Аниме", "style_anime")
                ),
                Arrays.asList(
                        botMessageBuilder.createInlineButton("🎨 Художественный", "style_artistic"),
                        botMessageBuilder.createInlineButton("🔥 Гранж", "style_grunge")
                ),
                Arrays.asList(
                         botMessageBuilder.createInlineButton("🚀 Киберпанк", "style_cyberpunk"),
                         botMessageBuilder.createInlineButton("🌅 Импрессионизм", "style_impressionism")
                ),
                Arrays.asList(
                         botMessageBuilder.createInlineButton("🖤 Готика", "style_gothic"),
                         botMessageBuilder.createInlineButton("🌈 Поп-арт", "style_pop_art")
                ),
                Arrays.asList(
                         botMessageBuilder.createInlineButton("🏛️ Классический", "style_classical"),
                         botMessageBuilder.createInlineButton("🌊 Абстракция", "style_abstract")
                ),
                Arrays.asList(
                         botMessageBuilder.createInlineButton("🎭 Сюрреализм", "style_surrealism"),
                         botMessageBuilder.createInlineButton("🏙️ Модерн", "style_modern")
                ),
                Arrays.asList(
                         botMessageBuilder.createInlineButton("✏️ Эскиз", "style_sketch"),
                         botMessageBuilder.createInlineButton("🖼️ Винтаж", "style_vintage")
                ),
                Arrays.asList(
                         botMessageBuilder.createInlineButton("✨ Свой стиль", "style_custom")
                )
        );
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("❌ Ошибка отправки клавиатуры стилей: {}", e.getMessage());
        }
    }

    private void askForFormat(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("📐 Выберите формат:");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = Arrays.asList(
                Arrays.asList(botMessageBuilder.createInlineButton("⬜ Квадрат (1:1)", "format_square")),
                Arrays.asList(botMessageBuilder.createInlineButton("📺 Широкий (16:9)", "format_wide")),
                Arrays.asList(botMessageBuilder.createInlineButton("📱 Вертикальный (9:16)", "format_vertical"))
        );
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("❌ Ошибка отправки клавиатуры форматов: {}", e.getMessage());
        }
    }

    private void handleStyleSelection(Long chatId, String userId, String callbackData) {
        GenerationRequestDTO dto = userGenerationCache.get(userId);
        if (dto == null) return;
        String style = callbackData.replace("style_", "");

        // Если выбран custom style, просим пользователя ввести свой
        if ("custom".equals(style)) {
            sendTextMessage(chatId, "✏️ Введите описание желаемого стиля (например: 'в стиле Ван Гога', 'минимализм', 'неон'):");
            // Помечаем, что ждем пользовательский стиль
            dto.setStyle("awaiting_custom");
            return;
        }

        dto.setStyle(style);

        sendTextMessage(chatId, "✅ Стиль выбран: " + getStyleDisplayName(style));

        // Переходим к следующему шагу
        if (dto.getAspectRatio() == null) {
            askForFormat(chatId);
        } else {
            showGenerationPreview(chatId, dto);
        }
    }

    private void handleFormatSelection(Long chatId, String userId, String callbackData) {
        GenerationRequestDTO dto = userGenerationCache.get(userId);
        if (dto == null) return;

        String format = callbackData.replace("format_", "");
        dto.setAspectRatio(getAspectRatioFromFormat(format));

        sendTextMessage(chatId, "✅ Формат выбран: " + getFormatDisplayName(format));

        // Автовыбор модели и показ превью
        autoSelectModel(dto);
        showGenerationPreview(chatId, dto);
    }



    private void showGenerationPreview(Long chatId, GenerationRequestDTO dto) {
        String preview = String.format("""
            👁️ **Предпросмотр генерации:**
            🎨 **Тип:** %s
            🎭 **Стиль:** %s
            📐 **Формат:** %s
            
            Всё верно?
            """,
                dto.getMediaType() == MediaType.IMAGE ? "Изображение" : "Видео",
                getStyleDisplayName(dto.getStyle()),
                dto.getAspectRatio()
        );

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(preview);
        message.setParseMode("Markdown");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = Arrays.asList(
                Arrays.asList(
                        botMessageBuilder.createInlineButton("✅ Создать", "confirm_generation"),
                        botMessageBuilder.createInlineButton("✏️ Изменить", "edit_parameters")
                )
        );
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("❌ Ошибка отправки превью: {}", e.getMessage());
        }
    }


    private void handleParameterEdit(Long chatId, String userId) {
        sendTextMessage(chatId, "✏️ Хорошо, давайте уточним параметры. Что хотите изменить?");
        askForStyle(chatId);
    }

    private void showRatingKeyboard(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("⭐ Оцените результат:");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<InlineKeyboardButton> row = Arrays.asList(
                botMessageBuilder.createInlineButton("1⭐", "rate_1"),
                botMessageBuilder.createInlineButton("2⭐", "rate_2"),
                botMessageBuilder.createInlineButton("3⭐", "rate_3"),
                botMessageBuilder.createInlineButton("4⭐", "rate_4"),
                botMessageBuilder.createInlineButton("5⭐", "rate_5")
        );
        keyboard.setKeyboard(Arrays.asList(row));
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("❌ Ошибка отправки клавиатуры рейтинга: {}", e.getMessage());
        }
    }

    private void handleRating(Long chatId, String callbackData) {
        int rating = Integer.parseInt(callbackData.replace("rate_", ""));

        // Здесь можно сохранить рейтинг в базу данных
        log.info("📊 Получен рейтинг {} от пользователя {}", rating, chatId);

        String response = rating >= 4 ?
                "🌟 Спасибо за высокую оценку! Рад, что результат вам понравился!" :
                "📝 Спасибо за оценку! Буду стараться лучше в следующий раз.";

        sendTextMessage(chatId, response);
    }

    private void handleTextChat(Long chatId, String messageText, ChatSession session) {
        String userId = String.valueOf(session.getUser().getTelegramId());
        GenerationRequestDTO dto = userGenerationCache.get(userId);

        // Проверяем, ждем ли мы пользовательский стиль
        if (dto != null && "awaiting_custom".equals(dto.getStyle())) {
            dto.setStyle(messageText);
            sendTextMessage(chatId, "✅ Ваш стиль сохранен: " + messageText);

            // Переходим к следующему шагу
            if (dto.getAspectRatio() == null) {
                askForFormat(chatId);
            } else {
                showGenerationPreview(chatId, dto);
            }
            return;
        }

        try {
            ChatCompletionResult response = chatService.generateResponseWithContext(
                    messageText, SYSTEM_PROMPT_CHAT, session);

            String botResponse = response.getChoices().get(0).getMessage().getContent();

            // Сохраняем ответ бота
            chatPersistenceService.saveMessage(session, Sender.BOT, botResponse, Bot.PHOTO_BOT, MessageType.TELEGRAM);

            sendTextMessage(chatId, botResponse);

        } catch (Exception e) {
            log.error("❌ Ошибка чата: {}", e.getMessage());
            sendErrorMessage(chatId, "Не удалось получить ответ. Попробуйте еще раз.");
        }
    }

    // Утилитарные методы
    private User getOrCreateUser(Message message) {
        return chatPersistenceService.getOrCreateUser(
                message.getFrom().getId().longValue(),
                message.getFrom().getUserName()
        );
    }

    private void sendTextMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("❌ Ошибка отправки сообщения: {}", e.getMessage());
        }
    }

    protected void sendErrorMessage(Long chatId, String errorText) {
        sendTextMessage(chatId, "❌ " + errorText);
    }

    private void sendPhoto(Long chatId, String photoUrl) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        sendPhoto.setPhoto(new InputFile(photoUrl));

        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            log.error("❌ Ошибка отправки фото: {}", e.getMessage());
            sendErrorMessage(chatId, "Не удалось отправить изображение.");
        }
    }

    private void sendVideo(Long chatId, String videoUrl) {
        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId);
        sendDocument.setDocument(new InputFile(videoUrl));

        try {
            execute(sendDocument);
        } catch (TelegramApiException e) {
            log.error("❌ Ошибка отправки видео: {}", e.getMessage());
            sendErrorMessage(chatId, "Не удалось отправить видео.");
        }
    }



    private String getStyleDisplayName(String style) {
        return switch (style) {
            case "realism" -> "📸 Реализм";
            case "anime" -> "🌸 Аниме";
            case "artistic" -> "🎨 Художественный";
            case "grunge" -> "🔥 Гранж";
            case "cyberpunk" -> "🚀 Киберпанк";
            case "impressionism" -> "🌅 Импрессионизм";
            case "gothic" -> "🖤 Готика";
            case "pop_art" -> "🌈 Поп-арт";
            case "classical" -> "🏛️ Классический";
            case "abstract" -> "🌊 Абстракция";
            case "surrealism" -> "🎭 Сюрреализм";
            case "modern" -> "🏙️ Модерн";
            case "sketch" -> "✏️ Эскиз";
            case "vintage" -> "🖼️ Винтаж";
            case "awaiting_custom" -> "⏳ Ожидание ввода...";
            default -> "✨ " + style; // Для пользовательских стилей
        };
    }

    private String getFormatDisplayName(String format) {
        return switch (format) {
            case "square" -> "Квадрат (1:1)";
            case "wide" -> "Широкий (16:9)";
            case "vertical" -> "Вертикальный (9:16)";
            default -> format;
        };
    }

    private String getAspectRatioFromFormat(String format) {
        return switch (format) {
            case "square" -> "1:1";
            case "wide" -> "16:9";
            case "vertical" -> "9:16";
            default -> "1:1";
        };
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