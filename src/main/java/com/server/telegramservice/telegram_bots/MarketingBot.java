package com.server.telegramservice.telegram_bots;

import com.server.telegramservice.entity.telegram.User;
import com.server.telegramservice.service.ai.chat.ChatService;
import com.server.telegramservice.telegram_bots.chat.impl.ChatPersistenceService;
import com.server.telegramservice.service.tgchat.CommandHandlerService;
import com.server.telegramservice.service.ImageCacheService;
import com.server.telegramservice.service.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.nio.file.Files;

// Импорт оставляем как есть

@Slf4j
@Component
public class MarketingBot extends TelegramLongPollingBot {

    private final ChatService openAiChatService;
    private final CommandHandlerService commandHandler;
    private final ChatPersistenceService chatPersistenceService;
    private final S3Service s3Service;
    private final ImageCacheService imageCacheService;

    @Value("${telegrambots.bots[1].username}")
    private String botUsername;

    @Value("${telegrambots.bots[1].token}")
    private String botToken;

    public MarketingBot(@Qualifier("openAiChatService") ChatService openAiChatService,
                        CommandHandlerService commandHandler,
                        ChatPersistenceService chatPersistenceService,
                        S3Service s3Service,
                        ImageCacheService imageCacheService) {
        this.openAiChatService = openAiChatService;
        this.commandHandler = commandHandler;
        this.chatPersistenceService = chatPersistenceService;
        this.s3Service = s3Service;
        this.imageCacheService = imageCacheService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) return;

        var message = update.getMessage();
        var chatId = message.getChatId();
        var telegramId = message.getFrom().getId();
        var username = message.getFrom().getUserName();

        User user = chatPersistenceService.getOrCreateUser(telegramId, username);

        if (message.hasPhoto()) {
           // handleIncomingPhoto(update, user, chatId);
            return;
        }

        if (message.hasText()) {
            String inputText = message.getText().trim();

//            if (pendingUploads.containsKey(telegramId)) {
//                handlePendingUploadConfirmation(user, chatId, telegramId, inputText);
//                return;
//            }

            handleTextMessage(user, chatId, inputText, username);
        }
    }

    private void handleTextMessage(User user, Long chatId, String userInput, String username) {
        String reply;
        if (userInput.startsWith("/")) {
            reply = commandHandler.handleCommand(userInput, username);
            if (userInput.equalsIgnoreCase("/newsession")) {
                chatPersistenceService.endActiveSession(user);
            }
        } else {
//            ChatSession session = chatPersistenceService.getOrCreateActiveSession(user);
//            chatPersistenceService.saveMessage(session, Sender.USER, userInput);

            String systemPrompt = "Ты профессиональный digital-маркетолог с 10+ годами опыта. " +
                    "Специализируешься на креативах, воронках, FOMO, CTA и ROI. Пиши кратко, ясно и с эмодзи 🎯💰🔥.";
//
////            var response = openAiChatService.generateResponseWithContext(userInput, systemPrompt, session);
//            reply = response.getChoices().get(0).getMessage().getContent();

//            chatPersistenceService.saveMessage(session, Sender.BOT, reply);
        }
//        sendSimpleMessage(chatId, reply);
    }

    // Остальные методы — handleIncomingPhoto, confirmPhoto и т.д. — могут быть оставлены как есть,
    // либо адаптированы под openAiChatService.generateResponseWithContext при необходимости

    private void sendSimpleMessage(Long chatId, String text) {
        try {
            SendMessage message = new SendMessage(chatId.toString(), text);
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения", e);
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

    private static class PendingUpload {
        String redisKey;
        String photoFileId;
        String caption;

        public PendingUpload(String redisKey, String photoFileId, String caption) {
            this.redisKey = redisKey;
            this.photoFileId = photoFileId;
            this.caption = caption;
        }
    }
    public byte[] downloadFileAsByteArray(String fileId) throws TelegramApiException, IOException {

        GetFile getFile = new GetFile(fileId);
        org.telegram.telegrambots.meta.api.objects.File fileInfo = execute(getFile);
        String filePath = fileInfo.getFilePath();         // Telegram's internal file path:contentReference[oaicite:8]{index=8}

        // 2. Download the file using the obtained file path
        java.io.File downloadedFile = downloadFile(filePath);  // this saves the file locally and returns it:contentReference[oaicite:9]{index=9}

        // 3. Read the downloaded file into a byte array
        byte[] fileBytes = Files.readAllBytes(downloadedFile.toPath());
        return fileBytes;
    }

}

