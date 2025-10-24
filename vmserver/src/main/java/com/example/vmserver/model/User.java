package com.example.vmserver.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
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
    @NotBlank
    private String login;

    //Электронная почта
    @Column(nullable = false)
    private String email;

    //Пароль
    @Column(nullable = false, name = "hash_password")
    @JsonIgnore
    private String hashpassword;

    @ManyToMany
    private List<VMStation> vmStations;
}