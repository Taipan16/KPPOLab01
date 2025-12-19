package com.example.vmserver.model;

import java.time.LocalDateTime;

import com.example.vmserver.enums.TokenType;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Модель токена для аутентификации и авторизации")
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(
        description = "Уникальный идентификатор токена",
        example = "1",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long id;

    @Schema(
        description = "Тип токена (ACCESS, REFRESH)",
        example = "ACCESS"
    )
    private TokenType type;

    @Schema(
        description = "Значение токена",
        example = "jhjdfgklddflgldfksgliueshpiurhfjesdicjvcdoprsihjviudehbviludhigudrfhiuyghdf"
    )
    private String value;

    @Schema(
        description = "Cрок действия токена",
        example = "2024-12-31T23:59:59"
    )
    private LocalDateTime expiringDate;

    @Schema(
        description = "Флаг отключения токена",
        example = "false"
    )
    private boolean disabled;

    @ManyToOne
    @Schema(description = "Пользователь, которому принадлежит токен")
    private VMUser vmUser;

    public Token(TokenType type, String value, LocalDateTime expiringDate, boolean disabled, VMUser vmUser) {
        this.type = type;
        this.value = value;
        this.expiringDate = expiringDate;
        this.disabled = disabled;
        this.vmUser = vmUser;
    }
}