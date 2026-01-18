package com.example.vmserver.service;

import com.example.vmserver.model.UserTelegramChat;
import com.example.vmserver.model.VMUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Сервис для работы с Telegram ботом
 */
public interface TelegramBotService {
    
    /**
     * Зарегистрировать пользователя в боте
     * @param username Имя пользователя в системе
     * @param chatId ID Telegram чата
     * @return Зарегистрированная привязка
     */
    UserTelegramChat registerUser(String username, Long chatId);
    
    /**
     * Отменить регистрацию пользователя в боте
     * @param username Имя пользователя в системе
     */
    void unregisterUser(String username);
    
    /**
     * Отправить уведомление о изменении статуса VM
     * @param vmId ID виртуальной машины
     * @param oldStatus Старый статус
     * @param newStatus Новый статус
     * @param changedBy Пользователь, изменивший статус
     */
    void sendVMStatusChangeNotification(Long vmId, String oldStatus, String newStatus, String changedBy);
    
    /**
     * Отправить сообщение конкретному пользователю
     * @param username Имя пользователя
     * @param message Сообщение
     */
    void sendMessageToUser(String username, String message);
    
    /**
     * Отправить сообщение всем зарегистрированным администраторам
     * @param message Сообщение
     */
    void sendMessageToAllAdmins(String message);
    
    /**
     * Получить список всех зарегистрированных пользователей
     * @return Список привязок
     */
    List<UserTelegramChat> getAllRegisteredUsers();
    
    /**
     * Проверить, является ли пользователь администратором
     * @param username Имя пользователя
     * @return true если пользователь ADMIN
     */
    boolean isUserAdmin(String username);
    
    /**
     * Получить информацию о привязке пользователя
     * @param username Имя пользователя
     * @return Привязка или null если не найдена
     */
    UserTelegramChat getUserRegistration(String username);
}