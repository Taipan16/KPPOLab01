package com.example.vmserver.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация Telegram бота
 */
@Configuration
@ConfigurationProperties(prefix = "telegram.bot")
@Getter
@Setter
public class BotConfig {
    
    /**
     * Токен бота полученный от @BotFather
     */
    private String token;
    
    /**
     * Имя бота (без @)
     */
    private String username;
    
    /**
     * Включен ли бот
     */
    private boolean enabled = true;
    
    /**
     * Отправлять уведомления только администраторам
     */
    private boolean notifyAdminsOnly = true;
}