package com.example.vmserver.service;

import com.example.vmserver.config.BotConfig;
import com.example.vmserver.enums.VMState;
import com.example.vmserver.model.UserTelegramChat;
import com.example.vmserver.model.VMUser;
import com.example.vmserver.repository.UserTelegramChatRepository;
import com.example.vmserver.repository.VMUserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TelegramBotServiceImpl extends TelegramLongPollingBot implements TelegramBotService {
    
    private final BotConfig botConfig;
    private final UserTelegramChatRepository userTelegramChatRepository;
    private final VMUserRepository userRepository;
    
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    
    @Override
    public String getBotUsername() {
        return botConfig.getUsername();
    }
    
    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }
    
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            
            if (messageText.startsWith("/start")) {
                sendWelcomeMessage(chatId);
            } else if (messageText.startsWith("/register")) {
                handleRegistration(chatId, messageText);
            } else if (messageText.startsWith("/unregister")) {
                handleUnregistration(chatId);
            } else if (messageText.startsWith("/help")) {
                sendHelpMessage(chatId);
            } else if (messageText.startsWith("/status")) {
                sendRegistrationStatus(chatId);
            } else {
                sendUnknownCommandMessage(chatId);
            }
        }
    }
    
    private void sendWelcomeMessage(Long chatId) {
        String message = """
            🤖 *Добро пожаловать в VM Status Bot!*
            
            Этот бот отправляет уведомления об изменении статуса виртуальных машин.
            
            *Доступные команды:*
            /register <логин> - Зарегистрироваться в системе
            /unregister - Отменить регистрацию
            /status - Проверить статус регистрации
            /help - Показать справку
            
            *Пример регистрации:*
            `/register admin_user`
            
            *Примечание:* Уведомления получают только пользователи с ролью ADMIN.
            """;
        
        sendTelegramMessage(chatId, message);
    }
    
    private void handleRegistration(Long chatId, String messageText) {
        try {
            String[] parts = messageText.split(" ");
            if (parts.length != 2) {
                sendTelegramMessage(chatId, "⚠️ *Неверный формат!*\nИспользуйте: /register <ваш_логин>");
                return;
            }
            
            String username = parts[1].trim();
            
            // Проверяем, существует ли пользователь
            VMUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));
            
            // Проверяем, является ли пользователь ADMIN
            if (!isUserAdmin(username)) {
                sendTelegramMessage(chatId, 
                    "⛔ *Отказ в регистрации!*\n" +
                    "Только пользователи с ролью ADMIN могут получать уведомления."
                );
                return;
            }
            
            // Проверяем, не зарегистрирован ли уже этот чат
            if (userTelegramChatRepository.existsByTelegramChatId(chatId)) {
                sendTelegramMessage(chatId, 
                    "⚠️ *Этот чат уже зарегистрирован!*\n" +
                    "Используйте /unregister для отмены текущей регистрации."
                );
                return;
            }
            
            // Проверяем, не зарегистрирован ли уже этот пользователь
            if (userTelegramChatRepository.existsByUsername(username)) {
                sendTelegramMessage(chatId, 
                    "⚠️ *Пользователь уже зарегистрирован!*\n" +
                    "Используйте /unregister для отмены текущей регистрации."
                );
                return;
            }
            
            // Регистрируем пользователя
            UserTelegramChat registration = new UserTelegramChat();
            registration.setUsername(username);
            registration.setTelegramChatId(chatId);
            registration.setActive(true);
            registration.setRegisteredAt(LocalDateTime.now());
            
            userTelegramChatRepository.save(registration);
            
            String response = String.format("""
                ✅ *Регистрация успешна!*
                
                *Пользователь:* %s
                *Роль:* ADMIN
                *Дата регистрации:* %s
                
                Теперь вы будете получать уведомления об изменении статуса виртуальных машин.
                """, 
                username, 
                registration.getRegisteredAt().format(formatter)
            );
            
            sendTelegramMessage(chatId, response);
            
        } catch (EntityNotFoundException e) {
            sendTelegramMessage(chatId, "❌ *Ошибка!* Пользователь с таким логином не найден.");
        } catch (Exception e) {
            log.error("Ошибка при регистрации пользователя", e);
            sendTelegramMessage(chatId, "❌ *Ошибка регистрации!* Попробуйте позже.");
        }
    }
    
    private void handleUnregistration(Long chatId) {
        try {
            UserTelegramChat registration = userTelegramChatRepository.findByTelegramChatId(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Регистрация не найдена"));
            
            userTelegramChatRepository.delete(registration);
            
            sendTelegramMessage(chatId, 
                "✅ *Регистрация отменена!*\n" +
                "Вы больше не будете получать уведомления."
            );
            
        } catch (EntityNotFoundException e) {
            sendTelegramMessage(chatId, "ℹ️ *Вы не зарегистрированы в системе.*");
        } catch (Exception e) {
            log.error("Ошибка при отмене регистрации", e);
            sendTelegramMessage(chatId, "❌ *Ошибка отмены регистрации!*");
        }
    }
    
    private void sendHelpMessage(Long chatId) {
        String message = """
            📋 *Справка по командам:*
            
            /start - Начать работу с ботом
            /register <логин> - Зарегистрироваться для получения уведомлений
            /unregister - Отменить регистрацию
            /status - Проверить статус регистрации
            /help - Показать эту справку
            
            *Важно:*
            • Регистрация доступна только пользователям с ролью ADMIN
            • Каждый пользователь может зарегистрировать только один чат
            • Уведомления отправляются при изменении статуса виртуальных машин
            """;
        
        sendTelegramMessage(chatId, message);
    }
    
    private void sendRegistrationStatus(Long chatId) {
        try {
            UserTelegramChat registration = userTelegramChatRepository.findByTelegramChatId(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Не зарегистрирован"));
            
            String status = registration.getActive() ? "✅ Активна" : "⭕ Неактивна";
            
            String message = String.format("""
                📊 *Статус регистрации:*
                
                *Пользователь:* %s
                *Статус:* %s
                *Дата регистрации:* %s
                *Chat ID:* %d
                """,
                registration.getUsername(),
                status,
                registration.getRegisteredAt().format(formatter),
                registration.getTelegramChatId()
            );
            
            sendTelegramMessage(chatId, message);
            
        } catch (EntityNotFoundException e) {
            sendTelegramMessage(chatId, "ℹ️ *Вы не зарегистрированы в системе.*");
        }
    }
    
    private void sendUnknownCommandMessage(Long chatId) {
        sendTelegramMessage(chatId, 
            "❓ *Неизвестная команда!*\n" +
            "Используйте /help для просмотра доступных команд."
        );
    }
    
    private void sendTelegramMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.enableMarkdown(true);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения в Telegram", e);
        }
    }
    
    @Override
    @Transactional
    public UserTelegramChat registerUser(String username, Long chatId) {
        if (userTelegramChatRepository.existsByTelegramChatId(chatId)) {
            throw new IllegalStateException("Этот чат уже зарегистрирован");
        }
        
        if (userTelegramChatRepository.existsByUsername(username)) {
            throw new IllegalStateException("Пользователь уже зарегистрирован");
        }
        
        if (!isUserAdmin(username)) {
            throw new IllegalStateException("Только пользователи с ролью ADMIN могут регистрироваться");
        }
        
        UserTelegramChat registration = new UserTelegramChat();
        registration.setUsername(username);
        registration.setTelegramChatId(chatId);
        registration.setActive(true);
        registration.setRegisteredAt(LocalDateTime.now());
        
        return userTelegramChatRepository.save(registration);
    }
    
    @Override
    @Transactional
    public void unregisterUser(String username) {
        userTelegramChatRepository.deleteByUsername(username);
    }
    
    @Override
    @Transactional
    public void sendVMStatusChangeNotification(Long vmId, String oldStatus, String newStatus, String changedBy) {
        if (!botConfig.isEnabled() || !botConfig.isNotifyAdminsOnly()) {
            return;
        }
        
        List<UserTelegramChat> registrations = getAllRegisteredUsers();
        
        if (registrations.isEmpty()) {
            log.debug("Нет зарегистрированных пользователей для уведомления");
            return;
        }
        
        String message = String.format("""
            🔄 *Изменение статуса VM*
            
            *VM ID:* %d
            *Старый статус:* %s
            *Новый статус:* %s
            *Изменено пользователем:* %s
            *Время:* %s
            
            *Описание статусов:*
            • OFF - Выключена
            • ON - Включена
            • WORK - Работает пользователь
            • REPAIR - На ремонте
            • FREE - Свободна
            • DISCONNECT - Не в сети
            """,
            vmId,
            oldStatus,
            newStatus,
            changedBy,
            LocalDateTime.now().format(formatter)
        );
        
        for (UserTelegramChat registration : registrations) {
            if (registration.getActive() && isUserAdmin(registration.getUsername())) {
                try {
                    sendTelegramMessage(registration.getTelegramChatId(), message);
                    log.debug("Уведомление отправлено пользователю: {}", registration.getUsername());
                } catch (Exception e) {
                    log.error("Ошибка отправки уведомления пользователю: {}", registration.getUsername(), e);
                }
            }
        }
    }
    
    @Override
    @Transactional
    public void sendMessageToUser(String username, String message) {
        UserTelegramChat registration = userTelegramChatRepository.findByUsername(username)
            .orElseThrow(() -> new EntityNotFoundException("Пользователь не зарегистрирован в боте"));
        
        if (registration.getActive()) {
            sendTelegramMessage(registration.getTelegramChatId(), message);
        }
    }
    
    @Override
    @Transactional
    public void sendMessageToAllAdmins(String message) {
        List<UserTelegramChat> registrations = getAllRegisteredUsers();
        
        for (UserTelegramChat registration : registrations) {
            if (registration.getActive() && isUserAdmin(registration.getUsername())) {
                try {
                    sendTelegramMessage(registration.getTelegramChatId(), message);
                } catch (Exception e) {
                    log.error("Ошибка отправки сообщения пользователю: {}", registration.getUsername(), e);
                }
            }
        }
    }
    
    @Override
    public List<UserTelegramChat> getAllRegisteredUsers() {
        return userTelegramChatRepository.findAll();
    }
    
    @Override
    public boolean isUserAdmin(String username) {
        try {
            VMUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));
            
            return "ADMIN".equalsIgnoreCase(user.getRole().getAuthority());
        } catch (Exception e) {
            log.error("Ошибка проверки роли пользователя: {}", username, e);
            return false;
        }
    }
    
    @Override
    public UserTelegramChat getUserRegistration(String username) {
        return userTelegramChatRepository.findByUsername(username).orElse(null);
    }
}