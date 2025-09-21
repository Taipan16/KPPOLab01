package com.example.vmserver.model;

import com.example.vmserver.enums.VMState;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vm_stations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VMStation {
    //ID станции
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //IP адрес
    @Column(nullable = false, unique = true)
    private String ip;

    //Порт подключения RDP
    @Column(nullable = false)
    private int port;

    //Cостояние станции
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VMState state;

    //Логин учетной записи
    @Column(nullable = false)
    private String login;

    //Пароль учетной записи
    @Column(name = "hash_password", nullable = false)
    private String hashPassword;
}

