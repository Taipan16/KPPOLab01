package com.example.vmserver.controller;

import com.example.vmserver.dto.AssignStationRequest;
import com.example.vmserver.dto.ReleaseStationRequest;
import com.example.vmserver.dto.VMQueueDTO;
import com.example.vmserver.service.VMQueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
@Tag(name = "Управление очередью станций", description = "API для управления очередью виртуальных машин")
public class VMQueueController {
    
    private final VMQueueService queueService;
    
    @PostMapping("/assign")
    @PreAuthorize("hasAuthority('QUEUE:ASSING')")
    @Operation(summary = "Привязать станцию к пользователю", 
               description = "Создает запись в очереди, связывая пользователя со станцией и меняя статус станции на WORK")
    public ResponseEntity<VMQueueDTO> assignStationToUser(@RequestBody AssignStationRequest request) {
        VMQueueDTO assignedQueue = queueService.assignStationToUser(request.userId(), request.stationId());
        return ResponseEntity.ok(assignedQueue);
    }
    
    @PostMapping("/release")
    @PreAuthorize("hasAuthority('QUEUE:RELEASE')")
    @Operation(summary = "Освободить станцию", 
               description = "Освобождает станцию, меняя статус записи в очереди на неактивный и статус станции на FREE")
    public ResponseEntity<VMQueueDTO> releaseStation(@RequestBody ReleaseStationRequest request) {
        VMQueueDTO releasedQueue = queueService.releaseStation(request.queueId());
        return ResponseEntity.ok(releasedQueue);
    }
    
    @GetMapping("/inactive")
    @PreAuthorize("hasAuthority('QUEUE:GETINACTIVE')")
    @Operation(summary = "Получить все неактивные записи", 
               description = "Возвращает список всех записей в очереди со статусом Active = false")
    public ResponseEntity<List<VMQueueDTO>> getAllInactiveRecords() {
        List<VMQueueDTO> inactiveRecords = queueService.getAllInactiveRecords();
        return ResponseEntity.ok(inactiveRecords);
    }
    
    @GetMapping("/user/{username}/active")
    @PreAuthorize("hasAuthority('QUEUE:GETACTIVE')")
    @Operation(summary = "Получить активные станции пользователя", 
               description = "Возвращает список всех активных станций, связанных с пользователем по его логину")
    public ResponseEntity<List<VMQueueDTO>> getActiveStationsByUsername(@PathVariable String username) {
        List<VMQueueDTO> activeStations = queueService.getActiveStationsByUsername(username);
        return ResponseEntity.ok(activeStations);
    }
    
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('QUEUE:GETACTIVEALL')")
    @Operation(summary = "Получить все активные записи", 
               description = "Возвращает список всех активных записей в очереди")
    public ResponseEntity<List<VMQueueDTO>> getAllActiveRecords() {
        List<VMQueueDTO> activeRecords = queueService.getAllActiveRecords();
        return ResponseEntity.ok(activeRecords);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('QUEUE:GETID')")
    @Operation(summary = "Получить запись по ID", 
               description = "Возвращает запись из очереди по её идентификатору")
    public ResponseEntity<VMQueueDTO> getQueueRecordById(@PathVariable Long id) {
        VMQueueDTO queueRecord = queueService.getQueueRecordById(id);
        return ResponseEntity.ok(queueRecord);
    }
    
    @GetMapping("/station/{stationId}/occupied")
    @PreAuthorize("hasAuthority('QUEUE:OCCUPIED')")
    @Operation(summary = "Проверить занятость станции", 
               description = "Проверяет, занята ли станция каким-либо пользователем")
    public ResponseEntity<Boolean> isStationOccupied(@PathVariable Long stationId) {
        boolean isOccupied = queueService.isStationOccupied(stationId);
        return ResponseEntity.ok(isOccupied);
    }
    
    @GetMapping("/user/{userId}/active-record")
    @PreAuthorize("hasAuthority('QUEUE:ACTIVERECORD')")
    @Operation(summary = "Получить активную запись пользователя", 
               description = "Возвращает активную запись в очереди для указанного пользователя")
    public ResponseEntity<VMQueueDTO> getActiveRecordByUserId(@PathVariable Long userId) {
        VMQueueDTO activeRecord = queueService.getActiveRecordByUserId(userId);
        return ResponseEntity.ok(activeRecord);
    }
}