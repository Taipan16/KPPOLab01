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
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TelegramBotServiceImpl extends TelegramLongPollingBot implements TelegramBotService {
    
    private final BotConfig botConfig;
    private final UserTelegramChatRepository userTelegramChatRepository;
    private final VMUserRepository userRepository;
    
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    
    @Override
    public String getBotUsername() {
        log.debug("Получение имени бота: {}", botConfig.getUsername());
        return botConfig.getUsername();
    }
    
    @Override
    public String getBotToken() {
        log.debug("Получение токена бота (первые 10 символов): {}", 
                botConfig.getToken() != null ? botConfig.getToken().substring(0, Math.min(10, botConfig.getToken().length())) + "..." : "null");
        return botConfig.getToken();
    }
    
    @Override
    public void onUpdateReceived(Update update) {
        long startTime = System.currentTimeMillis();
        log.info("Получено обновление от Telegram (update_id: {})", update.getUpdateId());
        
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                Long chatId = update.getMessage().getChatId();
                String username = update.getMessage().getFrom().getUserName();
                String firstName = update.getMessage().getFrom().getFirstName();
                String lastName = update.getMessage().getFrom().getLastName();
                
                log.debug("Сообщение от чата {} (пользователь: {} {} @{}): {}", 
                        chatId, firstName, lastName, username, messageText);
                
                if (messageText.startsWith("/start")) {
                    log.info("Обработка команды /start от чата {}", chatId);
                    sendWelcomeMessage(chatId);
                } else if (messageText.startsWith("/register")) {
                    log.info("Обработка команды /register от чата {}", chatId);
                    handleRegistration(chatId, messageText);
                } else if (messageText.startsWith("/unregister")) {
                    log.info("Обработка команды /unregister от чата {}", chatId);
                    handleUnregistration(chatId);
                } else if (messageText.startsWith("/help")) {
                    log.info("Обработка команды /help от чата {}", chatId);
                    sendHelpMessage(chatId);
                } else if (messageText.startsWith("/status")) {
                    log.info("Обработка команды /status от чата {}", chatId);
                    sendRegistrationStatus(chatId);
                } else {
                    log.warn("Неизвестная команда от чата {}: {}", chatId, messageText);
                    sendUnknownCommandMessage(chatId);
                }
            } else if (update.hasCallbackQuery()) {
                log.debug("Получен callback query: {}", update.getCallbackQuery().getData());
            } else {
                log.debug("Получено обновление без текстового сообщения: {}", update);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Обработка обновления {} завершена за {} мс", update.getUpdateId(), duration);
        } catch (Exception e) {
            log.error("Ошибка при обработке обновления {}: {}", update.getUpdateId(), e.getMessage(), e);
            throw e;
        }
    }
    
    private void sendWelcomeMessage(Long chatId) {
        log.debug("Отправка приветственного сообщения в чат {}", chatId);
        long startTime = System.currentTimeMillis();
        
        try {
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
            
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Приветственное сообщение отправлено в чат {} за {} мс", chatId, duration);
        } catch (Exception e) {
            log.error("Ошибка при отправке приветственного сообщения в чат {}: {}", chatId, e.getMessage(), e);
            throw e;
        }
    }
    
    private void handleRegistration(Long chatId, String messageText) {
        log.info("Обработка регистрации для чата {}", chatId);
        long startTime = System.currentTimeMillis();
        
        try {
            String[] parts = messageText.split(" ");
            if (parts.length != 2) {
                log.warn("Неверный формат команды регистрации от чата {}: {}", chatId, messageText);
                sendTelegramMessage(chatId, "⚠️ *Неверный формат!*\nИспользуйте: /register <ваш_логин>");
                return;
            }
            
            String username = parts[1].trim();
            log.debug("Попытка регистрации пользователя {} для чата {}", username, chatId);
            
            // Проверяем, существует ли пользователь
            VMUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Пользователь {} не найден при регистрации чата {}", username, chatId);
                    return new EntityNotFoundException("Пользователь не найден");
                });
            
            log.debug("Пользователь {} найден, проверка роли", username);
            
            // Проверяем, является ли пользователь ADMIN
            if (!isUserAdmin(username)) {
                log.warn("Попытка регистрации не-админа {} для чата {}", username, chatId);
                sendTelegramMessage(chatId, 
                    "⛔ *Отказ в регистрации!*\n" +
                    "Только пользователи с ролью ADMIN могут получать уведомления."
                );
                return;
            }
            
            // Проверяем, не зарегистрирован ли уже этот чат
            if (userTelegramChatRepository.existsByTelegramChatId(chatId)) {
                log.warn("Чат {} уже зарегистрирован в системе", chatId);
                sendTelegramMessage(chatId, 
                    "⚠️ *Этот чат уже зарегистрирован!*\n" +
                    "Используйте /unregister для отмены текущей регистрации."
                );
                return;
            }
            
            // Проверяем, не зарегистрирован ли уже этот пользователь
            if (userTelegramChatRepository.existsByUsername(username)) {
                log.warn("Пользователь {} уже зарегистрирован в другом чате", username);
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
            
            log.info("Пользователь {} успешно зарегистрирован для чата {}", username, chatId);
            
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
            
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Регистрация для чата {} завершена за {} мс", chatId, duration);
            
        } catch (EntityNotFoundException e) {
            log.error("Пользователь не найден при регистрации чата {}: {}", chatId, e.getMessage());
            sendTelegramMessage(chatId, "❌ *Ошибка!* Пользователь с таким логином не найден.");
        } catch (Exception e) {
            log.error("Ошибка при регистрации пользователя для чата {}: {}", chatId, e.getMessage(), e);
            sendTelegramMessage(chatId, "❌ *Ошибка регистрации!* Попробуйте позже.");
        }
    }
    
    private void handleUnregistration(Long chatId) {
        log.info("Обработка отмены регистрации для чата {}", chatId);
        long startTime = System.currentTimeMillis();
        
        try {
            UserTelegramChat registration = userTelegramChatRepository.findByTelegramChatId(chatId)
                .orElseThrow(() -> {
                    log.warn("Попытка отмены несуществующей регистрации для чата {}", chatId);
                    return new EntityNotFoundException("Регистрация не найдена");
                });
            
            String username = registration.getUsername();
            userTelegramChatRepository.delete(registration);
            
            log.info("Регистрация пользователя {} отменена для чата {}", username, chatId);
            
            sendTelegramMessage(chatId, 
                "✅ *Регистрация отменена!*\n" +
                "Вы больше не будете получать уведомления."
            );
            
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Отмена регистрации для чата {} завершена за {} мс", chatId, duration);
            
        } catch (EntityNotFoundException e) {
            log.debug("Пользователь не зарегистрирован в чате {}", chatId);
            sendTelegramMessage(chatId, "ℹ️ *Вы не зарегистрированы в системе.*");
        } catch (Exception e) {
            log.error("Ошибка при отмене регистрации для чата {}: {}", chatId, e.getMessage(), e);
            sendTelegramMessage(chatId, "❌ *Ошибка отмены регистрации!*");
        }
    }
    
    private void sendHelpMessage(Long chatId) {
        log.debug("Отправка справки в чат {}", chatId);
        long startTime = System.currentTimeMillis();
        
        try {
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
            
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Справка отправлена в чат {} за {} мс", chatId, duration);
        } catch (Exception e) {
            log.error("Ошибка при отправке справки в чат {}: {}", chatId, e.getMessage(), e);
            throw e;
        }
    }
    
    private void sendRegistrationStatus(Long chatId) {
        log.debug("Отправка статуса регистрации для чата {}", chatId);
        long startTime = System.currentTimeMillis();
        
        try {
            UserTelegramChat registration = userTelegramChatRepository.findByTelegramChatId(chatId)
                .orElseThrow(() -> {
                    log.debug("Статус запрошен для незарегистрированного чата {}", chatId);
                    return new EntityNotFoundException("Не зарегистрирован");
                });
            
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
            
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Статус регистрации отправлен в чат {} за {} мс", chatId, duration);
            
        } catch (EntityNotFoundException e) {
            log.debug("Пользователь не зарегистрирован в чате {}", chatId);
            sendTelegramMessage(chatId, "ℹ️ *Вы не зарегистрированы в системе.*");
        } catch (Exception e) {
            log.error("Ошибка при отправке статуса регистрации в чат {}: {}", chatId, e.getMessage(), e);
            throw e;
        }
    }
    
    private void sendUnknownCommandMessage(Long chatId) {
        log.debug("Отправка сообщения о неизвестной команде в чат {}", chatId);
        
        try {
            sendTelegramMessage(chatId, 
                "❓ *Неизвестная команда!*\n" +
                "Используйте /help для просмотра доступных команд."
            );
        } catch (Exception e) {
            log.error("Ошибка при отправке сообщения о неизвестной команде в чат {}: {}", chatId, e.getMessage(), e);
            throw e;
        }
    }
    
    private void sendTelegramMessage(Long chatId, String text) {
        log.debug("Отправка Telegram сообщения в чат {} (длина текста: {})", chatId, text.length());
        long startTime = System.currentTimeMillis();
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.enableMarkdown(true);
        
        try {
            execute(message);
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Сообщение успешно отправлено в чат {} за {} мс", chatId, duration);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения в Telegram чат {}: {}", chatId, e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public UserTelegramChat registerUser(String username, Long chatId) {
        log.info("API вызов registerUser: username={}, chatId={}", username, chatId);
        long startTime = System.currentTimeMillis();
        
        try {
            if (userTelegramChatRepository.existsByTelegramChatId(chatId)) {
                log.warn("Попытка регистрации уже зарегистрированного чата {}", chatId);
                throw new IllegalStateException("Этот чат уже зарегистрирован");
            }
            
            if (userTelegramChatRepository.existsByUsername(username)) {
                log.warn("Попытка регистрации уже зарегистрированного пользователя {}", username);
                throw new IllegalStateException("Пользователь уже зарегистрирован");
            }
            
            if (!isUserAdmin(username)) {
                log.warn("Попытка регистрации не-админа {}", username);
                throw new IllegalStateException("Только пользователи с ролью ADMIN могут регистрироваться");
            }
            
            UserTelegramChat registration = new UserTelegramChat();
            registration.setUsername(username);
            registration.setTelegramChatId(chatId);
            registration.setActive(true);
            registration.setRegisteredAt(LocalDateTime.now());
            
            UserTelegramChat savedRegistration = userTelegramChatRepository.save(registration);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("API регистрация пользователя {} для чата {} завершена за {} мс", username, chatId, duration);
            
            return savedRegistration;
        } catch (Exception e) {
            log.error("Ошибка API регистрации пользователя {} для чата {}: {}", username, chatId, e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    @Transactional
    public void unregisterUser(String username) {
        log.info("API вызов unregisterUser для пользователя {}", username);
        long startTime = System.currentTimeMillis();
        
        try {
            Integer deletedCount = userTelegramChatRepository.deleteByUsername(username);
            
            if (deletedCount > 0) {
                log.info("API отмена регистрации пользователя {} успешна, удалено {} записей", username, deletedCount);
            } else {
                log.warn("API отмена регистрации: пользователь {} не найден", username);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.debug("API отмена регистрации пользователя {} завершена за {} мс", username, duration);
        } catch (Exception e) {
            log.error("Ошибка API отмены регистрации пользователя {}: {}", username, e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    @Transactional
    public void sendVMStatusChangeNotification(Long vmId, String oldStatus, String newStatus, String changedBy) {
        log.info("Отправка уведомления об изменении статуса VM: vmId={}, oldStatus={}, newStatus={}, changedBy={}", 
                vmId, oldStatus, newStatus, changedBy);
        long startTime = System.currentTimeMillis();
        
        try {
            if (!botConfig.isEnabled()) {
                log.warn("Бот отключен в конфигурации, уведомление не отправлено");
                return;
            }
            
            if (!botConfig.isNotifyAdminsOnly()) {
                log.warn("Уведомления для не-админов отключены в конфигурации");
                return;
            }
            
            List<UserTelegramChat> registrations = getAllRegisteredUsers();
            log.debug("Найдено {} зарегистрированных пользователей", registrations.size());
            
            if (registrations.isEmpty()) {
                log.info("Нет зарегистрированных пользователей для уведомления");
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
            
            int sentCount = 0;
            int errorCount = 0;
            
            for (UserTelegramChat registration : registrations) {
                if (registration.getActive() && isUserAdmin(registration.getUsername())) {
                    try {
                        sendTelegramMessage(registration.getTelegramChatId(), message);
                        sentCount++;
                        log.debug("Уведомление отправлено пользователю: {}", registration.getUsername());
                    } catch (Exception e) {
                        errorCount++;
                        log.error("Ошибка отправки уведомления пользователю {}: {}", registration.getUsername(), e.getMessage(), e);
                    }
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Уведомление о статусе VM отправлено: {} успешно, {} с ошибками, за {} мс", 
                    sentCount, errorCount, duration);
            
        } catch (Exception e) {
            log.error("Ошибка отправки уведомления об изменении статуса VM: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    @Transactional
    public void sendMessageToUser(String username, String message) {
        log.info("API вызов sendMessageToUser: username={}, длина сообщения={}", username, message.length());
        long startTime = System.currentTimeMillis();
        
        try {
            UserTelegramChat registration = userTelegramChatRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Пользователь {} не зарегистрирован в боте для отправки сообщения", username);
                    return new EntityNotFoundException("Пользователь не зарегистрирован в боте");
                });
            
            if (registration.getActive()) {
                sendTelegramMessage(registration.getTelegramChatId(), message);
                log.info("Сообщение отправлено пользователю {}", username);
            } else {
                log.warn("Регистрация пользователя {} неактивна, сообщение не отправлено", username);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.debug("API отправка сообщения пользователю {} завершена за {} мс", username, duration);
        } catch (EntityNotFoundException e) {
            log.error("Пользователь {} не найден для отправки сообщения", username);
            throw e;
        } catch (Exception e) {
            log.error("Ошибка API отправки сообщения пользователю {}: {}", username, e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    @Transactional
    public void sendMessageToAllAdmins(String message) {
        log.info("API вызов sendMessageToAllAdmins, длина сообщения={}", message.length());
        long startTime = System.currentTimeMillis();
        
        try {
            List<UserTelegramChat> registrations = getAllRegisteredUsers();
            log.debug("Найдено {} зарегистрированных пользователей", registrations.size());
            
            int sentCount = 0;
            int errorCount = 0;
            
            for (UserTelegramChat registration : registrations) {
                if (registration.getActive() && isUserAdmin(registration.getUsername())) {
                    try {
                        sendTelegramMessage(registration.getTelegramChatId(), message);
                        sentCount++;
                    } catch (Exception e) {
                        errorCount++;
                        log.error("Ошибка отправки сообщения пользователю {}: {}", registration.getUsername(), e.getMessage(), e);
                    }
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Сообщение отправлено всем админам: {} успешно, {} с ошибками, за {} мс", 
                    sentCount, errorCount, duration);
            
        } catch (Exception e) {
            log.error("Ошибка API отправки сообщения всем админам: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public List<UserTelegramChat> getAllRegisteredUsers() {
        log.debug("API вызов getAllRegisteredUsers");
        long startTime = System.currentTimeMillis();
        
        try {
            List<UserTelegramChat> users = userTelegramChatRepository.findAll();
            
            long duration = System.currentTimeMillis() - startTime;
            log.debug("API getAllRegisteredUsers завершено за {} мс, найдено {} пользователей", duration, users.size());
            
            return users;
        } catch (Exception e) {
            log.error("Ошибка API получения всех зарегистрированных пользователей: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public boolean isUserAdmin(String username) {
        log.debug("Проверка роли пользователя: {}", username);
        long startTime = System.currentTimeMillis();
        
        try {
            VMUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Пользователь {} не найден при проверке роли", username);
                    return new EntityNotFoundException("Пользователь не найден");
                });
            
            boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole().getAuthority());
            
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Проверка роли пользователя {} завершена за {} мс, результат: {}", username, duration, isAdmin);
            
            return isAdmin;
        } catch (Exception e) {
            log.error("Ошибка проверки роли пользователя {}: {}", username, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public UserTelegramChat getUserRegistration(String username) {
        log.debug("API вызов getUserRegistration для пользователя {}", username);
        long startTime = System.currentTimeMillis();
        
        try {
            UserTelegramChat registration = userTelegramChatRepository.findByUsername(username).orElse(null);
            
            long duration = System.currentTimeMillis() - startTime;
            if (registration != null) {
                log.debug("API getUserRegistration найдено для пользователя {} за {} мс", username, duration);
            } else {
                log.debug("API getUserRegistration не найдено для пользователя {} за {} мс", username, duration);
            }
            
            return registration;
        } catch (Exception e) {
            log.error("Ошибка API получения регистрации пользователя {}: {}", username, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Проверка работоспособности бота
     * @return true если бот работает нормально
     */
    public boolean healthCheck() {
        log.debug("Выполнение health check для Telegram бота");
        
        try {
            boolean isEnabled = botConfig.isEnabled();
            boolean hasToken = botConfig.getToken() != null && !botConfig.getToken().isEmpty();
            boolean hasUsername = botConfig.getUsername() != null && !botConfig.getUsername().isEmpty();
            
            log.info("Health check бота: enabled={}, hasToken={}, hasUsername={}", 
                    isEnabled, hasToken, hasUsername);
            
            return isEnabled && hasToken && hasUsername;
        } catch (Exception e) {
            log.error("Ошибка при health check бота: {}", e.getMessage(), e);
            return false;
        }
    }
}