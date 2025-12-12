package com.example.vmserver.mapper;

import com.example.vmserver.dto.VMQueueDTO;
import com.example.vmserver.model.VMQueue;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VMQueueMapper {
    
    public static VMQueueDTO queueToQueueDTO(VMQueue queue) {
        if (queue == null) {
            return null;
        }
        
        return VMQueueDTO.builder()
                .id(queue.getId())
                .userId(queue.getCurrentUser() != null ? queue.getCurrentUser().getId() : null)
                .username(queue.getCurrentUser() != null ? queue.getCurrentUser().getUsername() : null)
                .stationId(queue.getVmStation() != null ? queue.getVmStation().getId() : null)
                .stationIp(queue.getVmStation() != null ? queue.getVmStation().getIp() : null)
                .active(queue.getActive())
                .createdAt(queue.getCreatedAt())
                .releasedAt(queue.getReleasedAt())
                .build();
    }
}