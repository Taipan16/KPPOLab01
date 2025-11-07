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


@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private TokenType type;

    private String value;

    private LocalDateTime expiringDate;

    private boolean disabled;

    @ManyToOne
    private VMUser vmUser;

    public Token(TokenType type, String value, LocalDateTime expiringDate, boolean disabled, VMUser vmUser) {
        this.type = type;
        this.value = value;
        this.expiringDate = expiringDate;
        this.disabled = disabled;
        this.vmUser = vmUser;
    }

}
