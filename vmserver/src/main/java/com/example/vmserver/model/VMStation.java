package com.example.vmserver.model;

import java.util.List;

import com.example.vmserver.enums.VMState;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vm_stations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Модель виртуальной машины (станции)")
public class VMStation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Уникальный идентификатор виртуальной машины", 
            example = "1",
            accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(nullable = false, unique = true)
    @Schema(description = "IP-адрес виртуальной машины", 
            example = "192.168.1.100",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String ip;

    @Column(nullable = false)
    @Schema(description = "Порт для RDP подключения", 
            example = "3389",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private int port;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "Текущее состояние виртуальной машины", 
            example = "OFF",
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"OFF", "ON", "WORK", "REPAIR", "FREE", "DISCONNECT"})
    private VMState state;

    @Column(nullable = false)
    @Schema(description = "Логин учетной записи для доступа к виртуальной машине", 
            example = "admin",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String login;

    @Column(name = "hash_password", nullable = false)
    @Schema(description = "Пароль учетной записи", 
            example = "Q12werty",
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.WRITE_ONLY)
    private String hashPassword;

    @ManyToMany
    @Schema(description = "Список пользователей")
    private List<VMUser> users;
}