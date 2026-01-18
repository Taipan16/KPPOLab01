package com.example.vmserver.controller;

import com.example.vmserver.model.UserTelegramChat;
import com.example.vmserver.service.TelegramBotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/telegram-bot")
@RequiredArgsConstructor
@Tag(name = "Управление Telegram ботом", description = "API для управления Telegram ботом уведомлений")
public class TelegramBotController {
    
    private final TelegramBotService telegramBotService;
    
    @GetMapping("/registrations")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Получить все регистрации", 
               description = "Возвращает список всех пользователей, зарегистрированных в Telegram боте")
    public ResponseEntity<List<UserTelegramChat>> getAllRegistrations() {
        List<UserTelegramChat> registrations = telegramBotService.getAllRegisteredUsers();
        return ResponseEntity.ok(registrations);
    }
    
    @GetMapping("/registrations/{username}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Получить регистрацию пользователя", 
               description = "Возвращает информацию о регистрации пользователя в Telegram боте")
    public ResponseEntity<UserTelegramChat> getUserRegistration(@PathVariable String username) {
        UserTelegramChat registration = telegramBotService.getUserRegistration(username);
        if (registration == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(registration);
    }
    
    @PostMapping("/notify/{username}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Отправить тестовое уведомление", 
               description = "Отправляет тестовое уведомление указанному пользователю через Telegram бот")
    public ResponseEntity<Void> sendTestNotification(
            @PathVariable String username,
            @RequestParam String message) {
        telegramBotService.sendMessageToUser(username, message);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/notify/all")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Отправить уведомление всем администраторам", 
               description = "Отправляет уведомление всем зарегистрированным администраторам через Telegram бот")
    public ResponseEntity<Void> sendNotificationToAllAdmins(@RequestParam String message) {
        telegramBotService.sendMessageToAllAdmins(message);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/registrations/{username}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Удалить регистрацию пользователя", 
               description = "Удаляет регистрацию пользователя в Telegram боте")
    public ResponseEntity<Void> deleteRegistration(@PathVariable String username) {
        telegramBotService.unregisterUser(username);
        return ResponseEntity.ok().build();
    }
}