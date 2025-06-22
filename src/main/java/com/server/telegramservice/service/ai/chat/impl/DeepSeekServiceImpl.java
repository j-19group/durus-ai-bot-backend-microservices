package com.server.telegramservice.service.ai.chat.impl;

import com.server.telegramservice.entity.telegram.ChatSession;
import com.server.telegramservice.service.ai.chat.ChatService;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import org.springframework.beans.factory.annotation.Value;

public class DeepSeekServiceImpl implements ChatService {
    @Value("${deepseek.api-key}")
    private String deepseekApiKey;

    @Override
    public ChatCompletionResult generateResponse(String clientPrompt, String systemContent) {
        return null;
    }

    @Override
    public ChatCompletionResult generateResponseWithContext(String clientPrompt, String systemContent, ChatSession session) {
        return null;
    }
}
