package com.example.vmserver.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    //ID пользователя
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Логин пользователя
    @Column(nullable = false, unique = true)
    private String login;

    //Электронная почта
    @Column(nullable = false)
    private String email;

    //Пароль
    @Column(nullable = false, name = "hash_password")
    private String hashpassword;
}
