package com.example.vmserver.model;

import java.util.Set;

import org.springframework.security.core.GrantedAuthority;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import io.swagger.v3.oas.annotations.media.ArraySchema;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Модель разрешения (права доступа) для операций с ресурсами")
public class Permission implements GrantedAuthority{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(
        description = "Уникальный идентификатор разрешения",
        example = "1",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long id;

    @Schema(
        description = "Ресурс (сущность), к которому применяется разрешение",
        example = "USER",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String resource;

    @Schema(
        description = "Операция, которая разрешена над ресурсом",
        example = "CREATE",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String operation;

    @ManyToMany(mappedBy = "permissions")
    @ArraySchema(
        schema = @Schema(description = "Роль, содержащая это разрешение"),
        arraySchema = @Schema(
            description = "Набор ролей, которые имеют данное разрешение",
            accessMode = Schema.AccessMode.READ_ONLY
        )
    )
    private Set<Role> roles;

    @Schema(
        description = "Возвращает authority в верхнем регистре",
        example = "USER:CREATE",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    @Override
    public String getAuthority(){
        return String.format("%s:%s", resource.toUpperCase(), operation.toUpperCase());
    }
}
