package com.example.vmserver.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VMQueueDTO {
    private Long id;
    private Long userId;
    private String username;
    private Long stationId;
    private String stationIp;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime releasedAt;
}
