package com.example.vmserver.service;

import com.example.vmserver.dto.VMQueueDTO;
import com.example.vmserver.enums.VMState;
import com.example.vmserver.exception.ResourceNotFoundException;
import com.example.vmserver.mapper.VMQueueMapper;
import com.example.vmserver.model.VMQueue;
import com.example.vmserver.model.VMStation;
import com.example.vmserver.model.VMUser;
import com.example.vmserver.repository.VMQueueRepository;
import com.example.vmserver.repository.VMStationRepository;
import com.example.vmserver.repository.VMUserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VMQueueServiceImpl implements VMQueueService {
    
    private final VMQueueRepository queueRepository;
    private final VMUserRepository userRepository;
    private final VMStationRepository stationRepository;
    
    @Override
    @Transactional
    @CacheEvict(value = {"VMQueue", "VMStation"}, allEntries = true)
    public VMQueueDTO assignStationToUser(Long userId, Long stationId) {
        // Проверка существования пользователя
        VMUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с ID " + userId + " не найден"));
        
        // Проверка существования станции
        VMStation station = stationRepository.findById(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Станция с ID " + stationId + " не найден"));
        
        // Проверка, что станция не занята
        if (queueRepository.existsByVmStationIdAndActiveTrue(stationId)) {
            throw new IllegalStateException("Станция уже занята другим пользователем");
        }
        
        // Проверка, что у пользователя нет активной станции
        if (queueRepository.existsByCurrentUserIdAndActiveTrue(userId)) {
            throw new IllegalStateException("У пользователя уже есть активная станция");
        }
        
        // Проверка, что станция в состоянии FREE (свободна)
        if (station.getState() != VMState.FREE) {
            throw new IllegalStateException("Станция не доступна для использования. Текущий статус: " + station.getState());
        }
        
        // Создание новой записи в очереди
        VMQueue queue = new VMQueue();
        queue.setCurrentUser(user);
        queue.setVmStation(station);
        queue.setActive(true);
        
        // Обновление статуса станции на WORK
        station.setState(VMState.WORK);
        stationRepository.save(station);
        
        // Сохранение записи в очереди
        VMQueue savedQueue = queueRepository.save(queue);
        
        return VMQueueMapper.queueToQueueDTO(savedQueue);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = {"VMQueue", "VMStation"}, allEntries = true)
    public VMQueueDTO releaseStation(Long queueId) {
        // Поиск записи в очереди
        VMQueue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new ResourceNotFoundException("Запись в очереди с ID " + queueId + " не найдена"));
        
        // Проверка, что запись активна
        if (!queue.getActive()) {
            throw new IllegalStateException("Станция уже освобождена");
        }
        
        // Обновление записи в очереди
        queue.setActive(false);
        queue.setReleasedAt(LocalDateTime.now());
        
        // Обновление статуса станции на FREE
        VMStation station = queue.getVmStation();
        station.setState(VMState.FREE);
        stationRepository.save(station);
        
        // Сохранение обновленной записи
        VMQueue updatedQueue = queueRepository.save(queue);
        
        return VMQueueMapper.queueToQueueDTO(updatedQueue);
    }
    
    @Override
    @Cacheable(value = "VMQueue", key = "'inactive'")
    public List<VMQueueDTO> getAllInactiveRecords() {
        List<VMQueue> inactiveRecords = queueRepository.findByActiveFalse();
        return inactiveRecords.stream()
                .map(VMQueueMapper::queueToQueueDTO)
                .toList();
    }
    
    @Override
    @Cacheable(value = "VMQueue", key = "#username + '-active'")
    public List<VMQueueDTO> getActiveStationsByUsername(String username) {
        // Поиск пользователя по логину
        VMUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с логином " + username + " не найден"));
        
        // Получение активных записей для пользователя
        List<VMQueue> activeRecords = queueRepository.findByCurrentUserIdAndActiveTrueOrderByCreatedAtDesc(user.getId());
        
        return activeRecords.stream()
                .map(VMQueueMapper::queueToQueueDTO)
                .toList();
    }
    
    @Override
    @Cacheable(value = "VMQueue", key = "'active'")
    public List<VMQueueDTO> getAllActiveRecords() {
        List<VMQueue> activeRecords = queueRepository.findByActiveTrue();
        return activeRecords.stream()
                .map(VMQueueMapper::queueToQueueDTO)
                .toList();
    }
    
    @Override
    @Cacheable(value = "VMQueue", key = "#id")
    public VMQueueDTO getQueueRecordById(Long id) {
        VMQueue queue = queueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Запись в очереди с ID " + id + " не найдена"));
        return VMQueueMapper.queueToQueueDTO(queue);
    }
    
    @Override
    public boolean isStationOccupied(Long stationId) {
        return queueRepository.existsByVmStationIdAndActiveTrue(stationId);
    }
    
    @Override
    @Cacheable(value = "VMQueue", key = "'user-' + #userId")
    public VMQueueDTO getActiveRecordByUserId(Long userId) {
        VMQueue queue = queueRepository.findByCurrentUserIdAndActiveTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Активная запись для пользователя с ID " + userId + " не найдена"));
        return VMQueueMapper.queueToQueueDTO(queue);
    }
}