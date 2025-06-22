package com.server.telegramservice.service.ai.chat;

import com.server.telegramservice.entity.telegram.ChatSession;
import com.theokanning.openai.completion.chat.ChatCompletionResult;


public interface ChatService {
    public ChatCompletionResult generateResponse(String clientPrompt, String systemContent) ;
    public ChatCompletionResult generateResponseWithContext(String clientPrompt, String systemContent, ChatSession session);
}
