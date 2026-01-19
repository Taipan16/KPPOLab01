package com.example.vmserver.config;

import com.example.vmserver.service.TelegramBotServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramBotInitializer {
    
    private final TelegramBotServiceImpl telegramBotService;
    private final BotConfig botConfig;
    
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (!botConfig.isEnabled()) {
            log.info("Telegram бот отключен в конфигурации");
            return;
        }
        
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramBotService);
            log.info("Telegram бот успешно инициализирован: @{}", botConfig.getUsername());
        }
        catch (TelegramApiException e) {
            log.error("Ошибка инициализации Telegram бота", e);
        }
    }
}