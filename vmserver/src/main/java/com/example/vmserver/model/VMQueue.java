package com.example.vmserver.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Tag(name = "VMQueue", description = "Запись в очереди на виртуальную машину")
@Schema(description = "Сущность очереди, связывающая пользователя и станцию виртуальной машины")
public class VMQueue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Уникальный идентификатор записи в очереди", example = "1")
    private Long id;
    
    @ManyToOne
    @Schema(description = "Пользователь, находящийся в очереди")
    private VMUser currentUser;
    
    @OneToOne
    @Schema(description = "Станция виртуальной машины, на которую пользователь стоит в очереди")
    private VMStation vmStation;
    
    @Column(nullable = false)
    @Schema(description = "Активна ли запись в очереди", example = "true")
    private Boolean active = true;
    
    @CreationTimestamp
    @Schema(description = "Дата и время создания записи", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;
    
    @Column
    @Schema(description = "Дата и время освобождения станции", example = "2024-01-15T11:45:00")
    private LocalDateTime releasedAt;
    
    public VMQueue(VMUser currentUser, VMStation vmStation, Boolean active) {
        this.currentUser = currentUser;
        this.vmStation = vmStation;
        this.active = active;
    }
    
    @PrePersist
    @Schema(hidden = true)
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (active == null) {
            active = true;
        }
    }
}