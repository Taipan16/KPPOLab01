package com.example.vmserver.model;

import java.util.Set;

import org.springframework.security.core.GrantedAuthority;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;


@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Модель роли пользователя в системе")
public class Role implements GrantedAuthority {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(
        description = "Идентификатор роли",
        example = "1",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long id;

    @Schema(
        description = "Наименование роли",
        example = "USER",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;

    @OneToMany(mappedBy = "role")
    @Schema(
        description = "Список пользователей, которым назначена данная роль",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    private Set<VMUser> users;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @ArraySchema(
        schema = @Schema(description = "Право доступа"),
        arraySchema = @Schema(description = "Набор разрешений, связанных с данной ролью")
    )
    private Set<Permission> permissions;

    @Override
    @Schema(
        description = "Возвращает authority роли в верхнем регистре",
        example = "ADMIN",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    public String getAuthority() {
        return name.toUpperCase();
    }
}