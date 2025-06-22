package com.server.telegramservice.telegram_bots.chat.impl;

import com.server.telegramservice.entity.enums.Bot;
import com.server.telegramservice.entity.enums.MessageType;
import com.server.telegramservice.entity.enums.Sender;
import com.server.telegramservice.entity.telegram.*;
import com.server.telegramservice.entity.repository.ChatSessionRepository;
import com.server.telegramservice.entity.repository.MessageRepository;
import com.server.telegramservice.entity.repository.FileRepository;
import com.server.telegramservice.entity.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatPersistenceService {

    private final UserRepository userRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final MessageRepository messageRepository;
    private final FileRepository fileRepository;

    public User getOrCreateUser(Long telegramId, String username) {
        return userRepository.findByTelegramId(telegramId)
                .map(user -> {
                    if (!user.getUsername().equals(username)) {
                        user.setUsername(username); // обновим имя
                    }
                    return userRepository.save(user);
                })
                .orElseGet(() -> {
                    User user = new User();
                    user.setTelegramId(telegramId);
                    user.setUsername(username);
                    return userRepository.save(user);
                });
    }

    public ChatSession getOrCreateActiveSession(User user) {
        return chatSessionRepository.findTopByUserAndEndedAtIsNullOrderByStartedAtDesc(user)
                .orElseGet(() -> {
                    ChatSession session = new ChatSession();
                    session.setUser(user);
                    return chatSessionRepository.save(session);
                });
    }

    public void saveMessage(ChatSession session, Sender sender, String text, Bot botType, MessageType messageType) {
        Message message = new Message();
        message.setSession(session);
        message.setSender(sender);
        message.setContent(text);
        message.setBotType(botType);
        message.setMessageType(messageType);
        messageRepository.save(message);
    }

    @Transactional
    public void saveMessageWithFile(ChatSession session, Sender sender, String text,
                                    String filename, String fileType, String s3Url, Bot botType, MessageType messageType) {
        Message message = new Message();
        message.setSession(session);
        message.setSender(sender);
        message.setBotType(botType);
        message.setMessageType(messageType);
        message.setContent(text);
        message.setContent(text);
        message = messageRepository.save(message);

        File file = new File();
        file.setFilename(filename);
        file.setFileType(fileType);
        file.setS3Url(s3Url);
        file.setMessage(message);
        fileRepository.save(file);

        message.getFiles().add(file);
        messageRepository.save(message);
    }
    public List<Message> getLastNMessages(ChatSession session, int n) {
        return messageRepository.findRecentMessagesBySession(session, PageRequest.of(0, n))
                .stream()
                .sorted(Comparator.comparing(Message::getCreatedAt))
                .toList();
    }

    public void endActiveSession(User user) {
        chatSessionRepository.findTopByUserAndEndedAtIsNullOrderByStartedAtDesc(user)
                .ifPresent(session -> {
                    session.setEndedAt(LocalDateTime.now());
                    chatSessionRepository.save(session);
                });
    }
}