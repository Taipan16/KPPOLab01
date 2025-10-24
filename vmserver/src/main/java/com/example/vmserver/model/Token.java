package com.example.vmserver.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.security.core.token.TokenService;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Data
@Entity
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private TokenService type;

    private String value;

    private LocalDateTime expingDate;

    private boolean disabled;

    @ManyToOne
    private VMUser vmUser;

    public Token(TokenService type, String value, LocalDateTime expingDate, boolean disabled, VMUser vmUser) {
        this.type = type;
        this.value = value;
        this.expingDate = expingDate;
        this.disabled = disabled;
        this.vmUser = vmUser;
    }
}
