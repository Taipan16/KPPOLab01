package com.example.vmserver.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_telegram_chats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Модель для привязки пользователя к Telegram чату")
public class UserTelegramChat {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(
        description = "Уникальный идентификатор привязки",
        example = "228337",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long id;
    
    @Column(nullable = false, unique = true)
    @Schema(
        description = "Имя пользователя (логин) в системе",
        example = "admin",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String username;
    
    @Column(name = "telegram_chat_id", nullable = false, unique = true)
    @Schema(
        description = "Идентификатор Telegram чата",
        example = "123456789",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long telegramChatId;
    
    @Column(nullable = false)
    @Schema(
        description = "Статус активности уведомлений",
        example = "true",
        defaultValue = "true"
    )
    private Boolean active = true;
    
    @Schema(
        description = "Дата и время регистрации привязки",
        example = "2024-01-15T10:30:00"
    )
    private java.time.LocalDateTime registeredAt;
    
    @PrePersist
    protected void onCreate() {
        registeredAt = java.time.LocalDateTime.now();
    }
}
