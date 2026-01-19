package com.example.vmserver.repository;

import com.example.vmserver.model.UserTelegramChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserTelegramChatRepository extends JpaRepository<UserTelegramChat, Long> {
    
    /**
     * Найти привязку по имени пользователя
     * @param username имя пользователя
     * @return Optional с привязкой или пустой
     */
    Optional<UserTelegramChat> findByUsername(String username);
    
    /**
     * Найти привязку по ID Telegram чата
     * @param telegramChatId ID Telegram чата
     * @return Optional с привязкой или пустой
     */
    Optional<UserTelegramChat> findByTelegramChatId(Long telegramChatId);
    
    /**
     * Проверить существование привязки по имени пользователя
     * @param username имя пользователя
     * @return true если привязка существует
     */
    boolean existsByUsername(String username);
    
    /**
     * Проверить существование привязки по ID Telegram чата
     * @param telegramChatId ID Telegram чата
     * @return true если привязка существует
     */
    boolean existsByTelegramChatId(Long telegramChatId);
    
    /**
     * Удалить привязку по имени пользователя
     * @param username имя пользователя
     */
    int deleteByUsername(String username);
}
