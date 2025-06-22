package com.server.telegramservice.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.web.client.RestTemplate;

@Configuration
public class OpenAIConfig {
    @Value("${openai.api-key}")
    private String apiKey;

    @Bean
    public OpenAiService openAiService() {
        return new OpenAiService(apiKey);
    }
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}

