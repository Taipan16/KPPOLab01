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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VMQueueServiceImpl implements VMQueueService {
    
    private static final Logger logger = LoggerFactory.getLogger(VMQueueServiceImpl.class);
    
    private final VMQueueRepository queueRepository;
    private final VMUserRepository userRepository;
    private final VMStationRepository stationRepository;
    private final TelegramBotService telegramBotService;

    @Override
    @Transactional
    @CacheEvict(value = {"VMQueue", "VMStation"}, allEntries = true)
    public VMQueueDTO assignStationToUser(Long userId, Long stationId) {
        logger.info("Начало назначения станции пользователю: userId={}, stationId={}", userId, stationId);
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            // Проверка существования пользователя
            logger.debug("Поиск пользователя с ID: {}", userId);
            VMUser user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        logger.error("Пользователь с ID {} не найден", userId);
                        return new ResourceNotFoundException("Пользователь с ID " + userId + " не найден");
                    });
            logger.debug("Пользователь найден: {}", user.getUsername());
            
            // Проверка существования станции
            logger.debug("Поиск станции с ID: {}", stationId);
            VMStation station = stationRepository.findById(stationId)
                    .orElseThrow(() -> {
                        logger.error("Станция с ID {} не найдена", stationId);
                        return new ResourceNotFoundException("Станция с ID " + stationId + " не найдена");
                    });
            logger.debug("Станция найдена: {}:{}", station.getIp(), station.getPort());
            
            // Проверка, что станция не занята
            logger.debug("Проверка занятости станции {}", stationId);
            if (queueRepository.existsByVmStationIdAndActiveTrue(stationId)) {
                logger.warn("Станция {} уже занята другим пользователем", stationId);
                throw new IllegalStateException("Станция уже занята другим пользователем");
            }
            
            // Проверка, что у пользователя нет активной станции
            logger.debug("Проверка активной станции у пользователя {}", userId);
            if (queueRepository.existsByCurrentUserIdAndActiveTrue(userId)) {
                logger.warn("У пользователя {} уже есть активная станция", user.getUsername());
                throw new IllegalStateException("У пользователя уже есть активная станция");
            }
            
            // Проверка, что станция в состоянии FREE (свободна)
            logger.debug("Проверка статуса станции: текущий статус={}, ожидаемый=FREE", station.getState());
            if (station.getState() != VMState.FREE) {
                logger.warn("Станция {} не доступна для использования. Текущий статус: {}", stationId, station.getState());
                throw new IllegalStateException("Станция не доступна для использования. Текущий статус: " + station.getState());
            }
            
            // Создание новой записи в очереди
            logger.debug("Создание новой записи в очереди");
            VMQueue queue = new VMQueue(user, station, true);
            
            // Обновление статуса станции на WORK
            logger.debug("Обновление статуса станции с FREE на WORK");
            station.setState(VMState.WORK);
            stationRepository.save(station);
            
            // Сохранение записи в очереди
            logger.debug("Сохранение записи в очереди");
            VMQueue savedQueue = queueRepository.save(queue);
            logger.info("Запись в очереди создана с ID: {}", savedQueue.getId());
            
            // Отправка уведомления в Telegram
            logger.debug("Отправка уведомления в Telegram об изменении статуса станции");
            try {
                String changedBy = user.getUsername();
                telegramBotService.sendVMStatusChangeNotification(
                    stationId,
                    VMState.FREE.toString(),
                    VMState.WORK.toString(),
                    changedBy
                );
                logger.info("Уведомление Telegram отправлено об изменении статуса станции {} на WORK", stationId);
            } catch (Exception e) {
                logger.error("Ошибка отправки уведомления в Telegram", e);
            }
            
            VMQueueDTO result = VMQueueMapper.queueToQueueDTO(savedQueue);
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.info("Назначение станции {} пользователю {} успешно завершено за {} мс. ID записи: {}", 
                    stationId, user.getUsername(), duration.toMillis(), savedQueue.getId());
            
            return result;
        } catch (Exception e) {
            logger.error("Ошибка при назначении станции {} пользователю {}: {}", 
                    stationId, userId, e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    @Transactional
    @CacheEvict(value = {"VMQueue", "VMStation"}, allEntries = true)
    public VMQueueDTO releaseStation(Long queueId) {
        logger.info("Начало освобождения станции по записи очереди: queueId={}", queueId);
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            // Поиск записи в очереди
            logger.debug("Поиск записи в очереди с ID: {}", queueId);
            VMQueue queue = queueRepository.findById(queueId)
                    .orElseThrow(() -> {
                        logger.error("Запись в очереди с ID {} не найдена", queueId);
                        return new ResourceNotFoundException("Запись в очереди с ID " + queueId + " не найдена");
                    });
            logger.debug("Запись в очереди найдена: пользователь={}, станция={}", 
                    queue.getCurrentUser().getUsername(), queue.getVmStation().getId());
            
            // Проверка, что запись активна
            logger.debug("Проверка активности записи: active={}", queue.getActive());
            if (!queue.getActive()) {
                logger.warn("Попытка освободить уже освобожденную станцию по записи {}", queueId);
                throw new IllegalStateException("Станция уже освобождена");
            }
            
            // Обновление записи в очереди
            logger.debug("Обновление записи в очереди: установка active=false");
            queue.setActive(false);
            queue.setReleasedAt(LocalDateTime.now());
            
            // Обновление статуса станции на FREE
            logger.debug("Обновление статуса станции с WORK на FREE");
            VMStation station = queue.getVmStation();
            station.setState(VMState.FREE);
            stationRepository.save(station);
            
            // Сохранение обновленной записи
            logger.debug("Сохранение обновленной записи в очереди");
            VMQueue updatedQueue = queueRepository.save(queue);
            logger.info("Запись в очереди {} обновлена: станция освобождена", queueId);
            
            // Отправка уведомления в Telegram
            logger.debug("Отправка уведомления в Telegram об освобождении станции");
            try {
                String changedBy = queue.getCurrentUser().getUsername();
                telegramBotService.sendVMStatusChangeNotification(
                    station.getId(),
                    VMState.WORK.toString(),
                    VMState.FREE.toString(),
                    changedBy
                );
                logger.info("Уведомление Telegram отправлено об изменении статуса станции {} на FREE", station.getId());
            } catch (Exception e) {
                logger.error("Ошибка отправки уведомления в Telegram", e);
            }
            
            VMQueueDTO result = VMQueueMapper.queueToQueueDTO(updatedQueue);
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.info("Освобождение станции по записи {} успешно завершено за {} мс", queueId, duration.toMillis());
            
            return result;
        } catch (Exception e) {
            logger.error("Ошибка при освобождении станции по записи {}: {}", queueId, e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    @Cacheable(value = "VMQueue", key = "'inactive'")
    public List<VMQueueDTO> getAllInactiveRecords() {
        logger.debug("Получение всех неактивных записей очереди");
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            List<VMQueue> inactiveRecords = queueRepository.findByActiveFalse();
            logger.debug("Найдено {} неактивных записей", inactiveRecords.size());
            
            List<VMQueueDTO> result = inactiveRecords.stream()
                    .map(VMQueueMapper::queueToQueueDTO)
                    .toList();
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.debug("Получение неактивных записей завершено за {} мс", duration.toMillis());
            
            return result;
        } catch (Exception e) {
            logger.error("Ошибка при получении неактивных записей: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    @Cacheable(value = "VMQueue", key = "#username + '-active'")
    public List<VMQueueDTO> getActiveStationsByUsername(String username) {
        logger.debug("Получение активных станций по имени пользователя: {}", username);
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            // Поиск пользователя по логину
            logger.debug("Поиск пользователя по логину: {}", username);
            VMUser user = userRepository.findByUsername(username)
                    .orElseThrow(() -> {
                        logger.error("Пользователь с логином {} не найден", username);
                        return new ResourceNotFoundException("Пользователь с логином " + username + " не найден");
                    });
            logger.debug("Пользователь найден: ID={}", user.getId());
            
            // Получение активных записей для пользователя
            logger.debug("Получение активных записей для пользователя ID={}", user.getId());
            List<VMQueue> activeRecords = queueRepository.findByCurrentUserIdAndActiveTrueOrderByCreatedAtDesc(user.getId());
            logger.debug("Найдено {} активных записей для пользователя {}", activeRecords.size(), username);
            
            List<VMQueueDTO> result = activeRecords.stream()
                    .map(VMQueueMapper::queueToQueueDTO)
                    .toList();
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.debug("Получение активных станций для пользователя {} завершено за {} мс", 
                    username, duration.toMillis());
            
            return result;
        } catch (Exception e) {
            logger.error("Ошибка при получении активных станций для пользователя {}: {}", 
                    username, e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    @Cacheable(value = "VMQueue", key = "'active'")
    public List<VMQueueDTO> getAllActiveRecords() {
        logger.debug("Получение всех активных записей очереди");
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            List<VMQueue> activeRecords = queueRepository.findByActiveTrue();
            logger.debug("Найдено {} активных записей", activeRecords.size());
            
            List<VMQueueDTO> result = activeRecords.stream()
                    .map(VMQueueMapper::queueToQueueDTO)
                    .toList();
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.debug("Получение всех активных записей завершено за {} мс", duration.toMillis());
            
            return result;
        } catch (Exception e) {
            logger.error("Ошибка при получении всех активных записей: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    @Cacheable(value = "VMQueue", key = "#id")
    public VMQueueDTO getQueueRecordById(Long id) {
        logger.debug("Получение записи очереди по ID: {}", id);
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            VMQueue queue = queueRepository.findById(id)
                    .orElseThrow(() -> {
                        logger.error("Запись в очереди с ID {} не найдена", id);
                        return new ResourceNotFoundException("Запись в очереди с ID " + id + " не найдена");
                    });
            
            logger.debug("Запись найдена: ID={}, активна={}, пользователь={}", 
                    id, queue.getActive(), queue.getCurrentUser().getUsername());
            
            VMQueueDTO result = VMQueueMapper.queueToQueueDTO(queue);
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.debug("Получение записи по ID {} завершено за {} мс", id, duration.toMillis());
            
            return result;
        } catch (Exception e) {
            logger.error("Ошибка при получении записи очереди по ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public boolean isStationOccupied(Long stationId) {
        logger.debug("Проверка занятости станции: stationId={}", stationId);
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            boolean occupied = queueRepository.existsByVmStationIdAndActiveTrue(stationId);
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.debug("Проверка занятости станции {} завершена за {} мс, результат: {}", 
                    stationId, duration.toMillis(), occupied);
            
            return occupied;
        } catch (Exception e) {
            logger.error("Ошибка при проверке занятости станции {}: {}", stationId, e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    @Cacheable(value = "VMQueue", key = "'user-' + #userId")
    public VMQueueDTO getActiveRecordByUserId(Long userId) {
        logger.debug("Получение активной записи по ID пользователя: {}", userId);
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            VMQueue queue = queueRepository.findByCurrentUserIdAndActiveTrue(userId)
                    .orElseThrow(() -> {
                        logger.warn("Активная запись для пользователя с ID {} не найдена", userId);
                        return new ResourceNotFoundException("Активная запись для пользователя с ID " + userId + " не найдена");
                    });
            
            logger.debug("Активная запись найдена: ID записи={}, станция={}", 
                    queue.getId(), queue.getVmStation().getId());
            
            VMQueueDTO result = VMQueueMapper.queueToQueueDTO(queue);
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.debug("Получение активной записи для пользователя {} завершено за {} мс", 
                    userId, duration.toMillis());
            
            return result;
        } catch (Exception e) {
            logger.error("Ошибка при получении активной записи для пользователя {}: {}", 
                    userId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Получить статистику по записям очереди
     * @return Статистика записей очереди
     */
    public QueueStatistics getQueueStatistics() {
        logger.debug("Получение статистики очереди");
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            long totalRecords = queueRepository.count();
            long activeRecords = queueRepository.countByActiveTrue();
            long inactiveRecords = queueRepository.countByActiveFalse();
            
            QueueStatistics statistics = new QueueStatistics(
                totalRecords, activeRecords, inactiveRecords
            );
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.debug("Статистика очереди получена за {} мс: всего={}, активных={}, неактивных={}", 
                    duration.toMillis(), totalRecords, activeRecords, inactiveRecords);
            
            return statistics;
        } catch (Exception e) {
            logger.error("Ошибка при получении статистики очереди: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Внутренний класс для статистики очереди
     */
    public static class QueueStatistics {
        private final long totalRecords;
        private final long activeRecords;
        private final long inactiveRecords;
        
        public QueueStatistics(long totalRecords, long activeRecords, long inactiveRecords) {
            this.totalRecords = totalRecords;
            this.activeRecords = activeRecords;
            this.inactiveRecords = inactiveRecords;
        }
        
        public long getTotalRecords() {
            return totalRecords;
        }
        
        public long getActiveRecords() {
            return activeRecords;
        }
        
        public long getInactiveRecords() {
            return inactiveRecords;
        }
        
        @Override
        public String toString() {
            return String.format("QueueStatistics{total=%d, active=%d, inactive=%d}", 
                    totalRecords, activeRecords, inactiveRecords);
        }
    }
}