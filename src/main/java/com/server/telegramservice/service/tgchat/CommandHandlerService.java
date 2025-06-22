package com.server.telegramservice.service.tgchat;

import org.springframework.stereotype.Service;

@Service
public class CommandHandlerService {

    public String handleCommand(String command, String username) {
        return switch (command.toLowerCase()) {
            case "/start" -> "Привет, " + username + "! Я — таргетолог-бот. Задавай вопросы про таргетированную рекламу.";
            case "/help" -> "Я могу помочь с:\n"
                    + "- планированием бюджета\n"
                    + "- настройкой рекламных кампаний\n"
                    + "- аналитикой и оптимизацией\n\n"
                    + "Просто задай вопрос, как профессионалу.";
            case "/newsession" -> "Новая сессия начата. Всё, что вы напишете далее, будет отдельной консультацией.";
            default -> "Неизвестная команда: " + command + "\nНапиши /help для списка команд.";
        };
    }
}

