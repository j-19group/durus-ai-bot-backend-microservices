package com.server.telegramservice.service.ai.chat.impl;

import com.server.telegramservice.service.ai.chat.ChatService;
import com.server.telegramservice.telegram_bots.chat.impl.ChatPersistenceService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.server.telegramservice.entity.telegram.ChatSession;
import com.server.telegramservice.entity.telegram.Message;
import com.server.telegramservice.entity.enums.Sender;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service("openAiChatService")
@RequiredArgsConstructor
@Slf4j
public class OpenAiServiceImpl implements ChatService {

    private final com.theokanning.openai.service.OpenAiService openAiService;
    private final ChatPersistenceService chatPersistenceService;

    @Value("${openai.api-key}")
    private String openAiApi;

    @Value("${openai.url}")
    private String openAiUrl;

    @Override
    public ChatCompletionResult generateResponse(String clientPrompt, String systemContent) {
        ChatMessage systemPrompt = new ChatMessage(ChatMessageRole.SYSTEM.value(), systemContent);
        ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(), clientPrompt);

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .messages(List.of(systemPrompt, userMessage))
                .temperature(0.7)
                .maxTokens(500)
                .build();

        try {
            return openAiService.createChatCompletion(request);
        } catch (Exception e) {
            log.error("Ошибка обращения к OpenAI: ");
            throw e;
        }
    }

    @Override
    public ChatCompletionResult generateResponseWithContext(String clientPrompt, String systemContent, ChatSession session) {
        List<ChatMessage> messages = new ArrayList<>();

        messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), systemContent));

        // 2. Add conversation history from session
        List<Message> conversationHistory = getConversationHistory(session, 8);

        for (Message msg : conversationHistory) {
            if (msg.getSender() == Sender.USER) {
                messages.add(new ChatMessage(ChatMessageRole.USER.value(), msg.getContent()));
            } else if (msg.getSender() == Sender.BOT) {
                messages.add(new ChatMessage(ChatMessageRole.ASSISTANT.value(), msg.getContent()));
            }
        }


        messages.add(new ChatMessage(ChatMessageRole.USER.value(), clientPrompt));
        // лимитируем сессий
        messages = limitContextWindow(messages, 1000); // Adjust token limit as needed

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .messages(messages)
                .temperature(0.7)
                .maxTokens(500)
                .build();

        try {
            return openAiService.createChatCompletion(request);
        } catch (Exception e) {
            log.error("Ошибка обращения к OpenAI с контекстом: " + e.getMessage());
            throw e;
        }
    }

    private List<Message> getConversationHistory(ChatSession session, int limit) {
        List<Message> recent = chatPersistenceService.getLastNMessages(session, limit);
        // Возвращаем в хронологическом порядке
        return recent.stream()
                .sorted(Comparator.comparing(Message::getCreatedAt))
                .toList();
    }


    private List<ChatMessage> limitContextWindow(List<ChatMessage> messages, int maxTokens) {

        if (messages.size() <= 10) {
            return messages;
        }

        List<ChatMessage> limited = new ArrayList<>();
        limited.add(messages.get(0)); // Always keep system prompt

        // Keep last 8 messages (4 exchanges) + current
        int startIndex = Math.max(1, messages.size() - 8);
        for (int i = startIndex; i < messages.size(); i++) {
            limited.add(messages.get(i));
        }

        return limited;
    }
}
