package com.example.vmserver.dto;

import com.example.vmserver.enums.VMState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO для системного отчёта")
public class ReportDTO {
    
    @Schema(description = "Дата и время формирования отчёта")
    private LocalDateTime reportDate;
    
    @Schema(description = "Общая статистика по станциям")
    private StationStatistics stationStatistics;
    
    @Schema(description = "Статистика по пользователям")
    private UserStatistics userStatistics;
    
    @Schema(description = "Последние записи в очереди")
    private List<QueueRecord> lastQueueRecords;
    
    @Schema(description = "Детальная информация по станциям")
    private List<StationDetail> stationDetails;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Статистика по станциям")
    public static class StationStatistics {
        @Schema(description = "Общее количество станций")
        private Integer totalStations;
        
        @Schema(description = "Количество станций в состоянии FREE")
        private Integer freeStations;
        
        @Schema(description = "Количество станций в состоянии WORK")
        private Integer workStations;
        
        @Schema(description = "Количество станций в состоянии ON")
        private Integer onStations;
        
        @Schema(description = "Количество станций в состоянии OFF")
        private Integer offStations;
        
        @Schema(description = "Количество станций в состоянии REPAIR")
        private Integer repairStations;
        
        @Schema(description = "Количество станций в состоянии DISCONNECT")
        private Integer disconnectStations;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Статистика по пользователям")
    public static class UserStatistics {
        @Schema(description = "Общее количество пользователей")
        private Integer totalUsers;
        
        @Schema(description = "Количество администраторов")
        private Integer adminCount;
        
        @Schema(description = "Количество обычных пользователей")
        private Integer userCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Запись в очереди")
    public static class QueueRecord {
        @Schema(description = "ID записи")
        private Long id;
        
        @Schema(description = "Имя пользователя")
        private String username;
        
        @Schema(description = "IP станции")
        private String stationIp;
        
        @Schema(description = "Дата создания")
        private LocalDateTime createdAt;
        
        @Schema(description = "Дата освобождения")
        private LocalDateTime releasedAt;
        
        @Schema(description = "Активна ли запись")
        private Boolean active;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Детальная информация о станции")
    public static class StationDetail {
        @Schema(description = "ID станции")
        private Long id;
        
        @Schema(description = "IP адрес")
        private String ip;
        
        @Schema(description = "Порт")
        private Integer port;
        
        @Schema(description = "Состояние")
        private VMState state;
        
        @Schema(description = "Логин")
        private String login;
        
        @Schema(description = "Текущий пользователь (если есть)")
        private String currentUser;
        
        @Schema(description = "Дата начала использования (если занята)")
        private LocalDateTime assignedAt;
    }
}