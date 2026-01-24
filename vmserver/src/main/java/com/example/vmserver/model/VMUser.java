package com.example.vmserver.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Модель пользователя системы")
public class VMUser implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Уникальный идентификатор пользователя", 
            example = "228", 
            accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(unique = true) 
    @Schema(description = "Имя пользователя для входа в систему", 
            example = "user",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "Пароль пользователя (хешированный)", 
            example = "Q12werty",
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;

    @ManyToOne
    @Schema(description = "Роль пользователя в системе")
    private Role role;

    @OneToMany(mappedBy = "vmUser")
    @Schema(description = "Токены аутентификации пользователя",
            accessMode = Schema.AccessMode.READ_ONLY)
    private Set<Token> tokens;

    @Override
    @Schema(hidden = true) // Скрываем в Swagger UI, так как это метод Spring Security
    public Collection<? extends GrantedAuthority> getAuthorities(){
        Set<String> authorities = new HashSet<>();
        role.getPermissions().forEach(p -> authorities.add(p.getAuthority()));
        authorities.add(role.getAuthority());
        return authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
    }
}