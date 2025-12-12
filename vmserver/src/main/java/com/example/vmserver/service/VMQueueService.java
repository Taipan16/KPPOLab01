package com.example.vmserver.service;

import com.example.vmserver.dto.VMQueueDTO;
import com.example.vmserver.model.VMQueue;

import java.util.List;

public interface VMQueueService {
    
    // Связать станцию с пользователем
    VMQueueDTO assignStationToUser(Long userId, Long stationId);
    
    // Освободить станцию
    VMQueueDTO releaseStation(Long queueId);
    
    // Получить все неактивные записи
    List<VMQueueDTO> getAllInactiveRecords();
    
    // Получить все активные станции для пользователя по логину
    List<VMQueueDTO> getActiveStationsByUsername(String username);
    
    // Получить все активные записи
    List<VMQueueDTO> getAllActiveRecords();
    
    // Получить запись по ID
    VMQueueDTO getQueueRecordById(Long id);
    
    // Проверить, занята ли станция
    boolean isStationOccupied(Long stationId);
    
    // Получить активную запись пользователя
    VMQueueDTO getActiveRecordByUserId(Long userId);
}