package com.server.telegramservice.configuration;

import com.server.telegramservice.telegram_bots.MarketingBot;
import com.server.telegramservice.telegram_bots.PhotoBot;
import com.server.telegramservice.telegram_bots.TargetingBot;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
@Configuration
public class TelegramBotConfig {

    private final TargetingBot targetingBot;
    private final MarketingBot marketingBot;
    private final PhotoBot photoBot;

    public TelegramBotConfig(TargetingBot targetingBot, MarketingBot marketingBot, PhotoBot photoBot) {
        this.targetingBot = targetingBot;
        this.marketingBot = marketingBot;
        this.photoBot = photoBot;
    }

    @PostConstruct
    public void registerBot() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(targetingBot);
            botsApi.registerBot(marketingBot);
            botsApi.registerBot(photoBot);
            log.info("✅ Telegram бот зарегистрирован успешно.");
        } catch (TelegramApiException e) {
            e.printStackTrace();
            log.error("❌ Ошибка регистрации бота: " + e.getMessage());
        }
    }
}
