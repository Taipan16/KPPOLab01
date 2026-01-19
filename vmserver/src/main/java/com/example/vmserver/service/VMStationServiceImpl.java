package com.example.vmserver.service;

import com.example.vmserver.enums.VMState;
import com.example.vmserver.model.VMStation;
import com.example.vmserver.repository.VMStationRepository;
import com.example.vmserver.specifications.VMStationSpecifications;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VMStationServiceImpl implements VMStationService {
    
    private static final Logger logger = LoggerFactory.getLogger(VMStationServiceImpl.class);
    
    private final VMStationRepository stationRepository;
    private final TelegramBotService telegramBotService;

    //Сохранение станции в БД
    @Transactional
    @CacheEvict(value = "VMStation", allEntries = true)
    @Override
    public VMStation createStation(VMStation station) {
        logger.info("Создание новой станции: IP={}, порт={}", station.getIp(), station.getPort());
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            // Проверяем, не существует ли уже станция с таким IP
            VMStation existingStation = stationRepository.findByIp(station.getIp());
            if (existingStation != null) {
                logger.warn("Попытка создать станцию с уже существующим IP: {}", station.getIp());
                throw new IllegalStateException("Станция с IP " + station.getIp() + " уже существует");
            }
            
            VMStation savedStation = stationRepository.save(station);
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.info("Станция успешно создана: ID={}, IP={}, порт={}, статус={}. Время выполнения: {} мс",
                    savedStation.getId(), savedStation.getIp(), savedStation.getPort(), 
                    savedStation.getState(), duration.toMillis());
            
            return savedStation;
        } catch (Exception e) {
            logger.error("Ошибка при создании станции: {}", e.getMessage(), e);
            throw e;
        }
    }

    //Удаление станции
    @Transactional
    @CacheEvict(value = "VMStation", allEntries = true)
    @Override
    public void deleteStation(Long id) {
        logger.info("Удаление станции с ID: {}", id);
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            VMStation station = stationRepository.findById(id)
                    .orElseThrow(() -> {
                        logger.error("Станция с ID {} не найдена для удаления", id);
                        return new EntityNotFoundException("Станция не найден по идентификатору: " + id);
                    });
            
            logger.debug("Найдена станция для удаления: IP={}, порт={}, статус={}", 
                    station.getIp(), station.getPort(), station.getState());
            
            stationRepository.delete(station);
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.info("Станция с ID {} успешно удалена. Время выполнения: {} мс", id, duration.toMillis());
            
        } catch (Exception e) {
            logger.error("Ошибка при удалении станции с ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }
    
    //Обновление полей станции
    @Transactional
    @CacheEvict(value = "VMStation", allEntries = true)
    @Override
    public VMStation updateStation(Long id, VMStation stationDetails) {
        logger.info("Обновление станции с ID: {}", id);
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            VMStation station = stationRepository.findById(id)
                    .orElseThrow(() -> {
                        logger.error("Станция с ID {} не найдена для обновления", id);
                        return new EntityNotFoundException("Станция не найден по идентификатору: " + id);
                    });
            
            // Сохраняем старый статус для уведомления
            VMState oldState = station.getState();
            VMState newState = stationDetails.getState();
            
            logger.debug("Текущие данные станции: IP={}, порт={}, статус={}", 
                    station.getIp(), station.getPort(), station.getState());
            logger.debug("Новые данные станции: IP={}, порт={}, статус={}", 
                    stationDetails.getIp(), stationDetails.getPort(), stationDetails.getState());
            
            // Проверяем изменение IP на уникальность, если IP меняется
            if (!station.getIp().equals(stationDetails.getIp())) {
                VMStation existingStation = stationRepository.findByIp(stationDetails.getIp());
                if (existingStation != null && !existingStation.getId().equals(id)) {
                    logger.warn("Попытка изменить IP станции на уже существующий: {}", stationDetails.getIp());
                    throw new IllegalStateException("Станция с IP " + stationDetails.getIp() + " уже существует");
                }
            }
            
            station.setIp(stationDetails.getIp());
            station.setPort(stationDetails.getPort());
            station.setState(newState);
            station.setLogin(stationDetails.getLogin());
            station.setHashPassword(stationDetails.getHashPassword());
            
            VMStation updatedStation = stationRepository.save(station);
            
            // Отправляем уведомление о изменении статуса, если статус изменился
            if (!oldState.equals(newState)) {
                logger.debug("Статус станции изменился: {} -> {}", oldState, newState);
                try {
                    String changedBy = SecurityContextHolder.getContext().getAuthentication().getName();
                    telegramBotService.sendVMStatusChangeNotification(
                        id,
                        oldState.toString(),
                        newState.toString(),
                        changedBy
                    );
                    logger.info("Уведомление Telegram отправлено об изменении статуса станции {}: {} -> {}", 
                            id, oldState, newState);
                } catch (Exception e) {
                    logger.error("Ошибка отправки уведомления в Telegram", e);
                }
            } else {
                logger.debug("Статус станции не изменился: {}", oldState);
            }
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.info("Станция с ID {} успешно обновлена. Время выполнения: {} мс", id, duration.toMillis());
            
            return updatedStation;
        } catch (Exception e) {
            logger.error("Ошибка при обновлении станции с ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    //Получение всех станций
    @Transactional
    @Cacheable(value = "VMStations")
    @Override
    public List<VMStation> getAllStations() {
        logger.debug("Получение всех станций");
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            List<VMStation> stations = stationRepository.findAll();
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.debug("Получено {} станций за {} мс", stations.size(), duration.toMillis());
            
            return stations;
        } catch (Exception e) {
            logger.error("Ошибка при получении всех станций: {}", e.getMessage(), e);
            throw e;
        }
    }

    //Получить станцию по ID
    @Transactional
    @Cacheable(value = "VMStation", key = "#id")
    @Override
    public VMStation getStationById(Long id) {
        logger.debug("Получение станции по ID: {}", id);
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            VMStation station = stationRepository.findById(id)
                    .orElseThrow(() -> {
                        logger.error("Станция с ID {} не найдена", id);
                        return new EntityNotFoundException("Станция не найден по идентификатору: " + id);
                    });
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.debug("Станция с ID {} найдена: IP={}, порт={}. Время поиска: {} мс", 
                    id, station.getIp(), station.getPort(), duration.toMillis());
            
            return station;
        } catch (Exception e) {
            logger.error("Ошибка при получении станции по ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public Page<VMStation> getByFilter(String login, Integer min, Integer max, Pageable pageable) {
        logger.debug("Фильтрация станций: login={}, min={}, max={}, pageable={}", 
                login, min, max, pageable);
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            Page<VMStation> result = stationRepository.findAll(
                    VMStationSpecifications.filter(login, min, max), pageable);
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.debug("Фильтрация завершена: найдено {} станций, {} страниц за {} мс", 
                    result.getTotalElements(), result.getTotalPages(), duration.toMillis());
            
            return result;
        } catch (Exception e) {
            logger.error("Ошибка при фильтрации станций: {}", e.getMessage(), e);
            throw e;
        }
    }

    
    @Override
    public String exportStationsToCsv() {
        logger.info("Экспорт станций в CSV формат");
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            List<VMStation> stations = stationRepository.findAll();
            logger.debug("Найдено {} станций для экспорта", stations.size());
            
            StringBuilder csvBuilder = new StringBuilder();
            
            // Заголовки CSV
            csvBuilder.append("ID,IP,Port,State,Login,HashPassword\n");
            
            // Данные станций
            for (VMStation station : stations) {
                csvBuilder.append(station.getId()).append(",");
                csvBuilder.append(station.getIp()).append(",");
                csvBuilder.append(station.getPort()).append(",");
                csvBuilder.append(station.getState()).append(",");
                csvBuilder.append(station.getLogin()).append(",");
                csvBuilder.append(station.getHashPassword()).append("\n");
            }
            
            String csvContent = csvBuilder.toString();
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.info("Экспорт станций в CSV завершен. Создано {} строк. Время выполнения: {} мс", 
                    stations.size() + 1, duration.toMillis());
            logger.debug("Размер CSV данных: {} символов", csvContent.length());
            
            return csvContent;
        } catch (Exception e) {
            logger.error("Ошибка при экспорте станций в CSV: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    @CacheEvict(value = {"VMStation", "VMStations"}, allEntries = true)
    @Override
    public String importStationsFromCsv(MultipartFile file) {
        logger.info("Импорт станций из CSV файла: {}", file.getOriginalFilename());
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            List<VMStation> importedStations = new ArrayList<>();
            int importedCount = 0;
            int updatedCount = 0;
            int skippedCount = 0;
            int errorCount = 0;
            
            logger.debug("Размер файла: {} байт", file.getSize());
            
            try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                String line;
                boolean isFirstLine = true;
                int lineNumber = 0;
                
                while ((line = br.readLine()) != null) {
                    lineNumber++;
                    
                    // Пропускаем заголовок
                    if (isFirstLine) {
                        isFirstLine = false;
                        logger.debug("Пропущена заголовочная строка CSV");
                        continue;
                    }
                    
                    String[] data = line.split(",");
                    if (data.length < 6) {
                        skippedCount++;
                        logger.warn("Строка {} пропущена: некорректный формат ({} полей вместо 6)", 
                                lineNumber, data.length);
                        continue; // Пропускаем некорректные строки
                    }
                    
                    try {
                        VMStation station = new VMStation();
                        
                        // ID игнорируем при импорте, т.к. он генерируется автоматически
                        // data[0] - ID
                        String ip = data[1].trim();
                        int port = Integer.parseInt(data[2].trim());
                        VMState state = VMState.valueOf(data[3].trim());
                        String login = data[4].trim();
                        String hashPassword = data[5].trim();
                        
                        logger.debug("Обработка строки {}: IP={}, порт={}, статус={}", 
                                lineNumber, ip, port, state);
                        
                        station.setIp(ip);
                        station.setPort(port);
                        station.setState(state);
                        station.setLogin(login);
                        station.setHashPassword(hashPassword);
                        
                        // Проверяем, существует ли станция с таким IP
                        VMStation existingStation = stationRepository.findByIp(station.getIp());
                        if (existingStation != null) {
                            // Обновляем существующую станцию
                            existingStation.setPort(station.getPort());
                            existingStation.setState(station.getState());
                            existingStation.setLogin(station.getLogin());
                            existingStation.setHashPassword(station.getHashPassword());
                            stationRepository.save(existingStation);
                            updatedCount++;
                            logger.debug("Станция с IP {} обновлена", station.getIp());
                        } else {
                            // Создаем новую станцию
                            stationRepository.save(station);
                            importedCount++;
                            logger.debug("Создана новая станция с IP {}", station.getIp());
                        }
                        
                    } catch (NumberFormatException e) {
                        errorCount++;
                        logger.error("Ошибка в строке {}: некорректный формат порта '{}'", 
                                lineNumber, data[2].trim(), e);
                    } catch (IllegalArgumentException e) {
                        errorCount++;
                        logger.error("Ошибка в строке {}: некорректный статус '{}'", 
                                lineNumber, data[3].trim(), e);
                    } catch (Exception e) {
                        errorCount++;
                        logger.error("Ошибка в строке {}: {}", lineNumber, e.getMessage(), e);
                    }
                }
            }
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            
            String result = String.format(
                "Импорт завершен. Успешно импортировано: %d, Обновлено: %d, Пропущено: %d, Ошибок: %d. Время выполнения: %d мс", 
                importedCount, updatedCount, skippedCount, errorCount, duration.toMillis()
            );
            
            logger.info(result);
            logger.debug("Импорт из файла {} завершен", file.getOriginalFilename());
            
            return result;
        } catch (Exception e) {
            logger.error("Ошибка при импорте CSV файла {}: {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new RuntimeException("Ошибка при импорте CSV файла: " + e.getMessage(), e);
        }
    }
    
    /**
     * Получить статистику по станциям
     * @return Статистика станций
     */
    public StationStatistics getStationStatistics() {
        logger.debug("Получение статистики по станциям");
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            long totalStations = stationRepository.count();
            long freeStations = stationRepository.countByState(VMState.FREE);
            long workStations = stationRepository.countByState(VMState.WORK);
            long onStations = stationRepository.countByState(VMState.ON);
            long offStations = stationRepository.countByState(VMState.OFF);
            long repairStations = stationRepository.countByState(VMState.REPAIR);
            long disconnectStations = stationRepository.countByState(VMState.DISCONNECT);
            
            StationStatistics statistics = new StationStatistics(
                totalStations, freeStations, workStations, onStations, 
                offStations, repairStations, disconnectStations
            );
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.debug("Статистика станций получена за {} мс: {}", duration.toMillis(), statistics);
            
            return statistics;
        } catch (Exception e) {
            logger.error("Ошибка при получении статистики станций: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Внутренний класс для статистики станций
     */
    public static class StationStatistics {
        private final long totalStations;
        private final long freeStations;
        private final long workStations;
        private final long onStations;
        private final long offStations;
        private final long repairStations;
        private final long disconnectStations;
        
        public StationStatistics(long totalStations, long freeStations, long workStations, 
                               long onStations, long offStations, long repairStations, 
                               long disconnectStations) {
            this.totalStations = totalStations;
            this.freeStations = freeStations;
            this.workStations = workStations;
            this.onStations = onStations;
            this.offStations = offStations;
            this.repairStations = repairStations;
            this.disconnectStations = disconnectStations;
        }
        
        public long getTotalStations() { return totalStations; }
        public long getFreeStations() { return freeStations; }
        public long getWorkStations() { return workStations; }
        public long getOnStations() { return onStations; }
        public long getOffStations() { return offStations; }
        public long getRepairStations() { return repairStations; }
        public long getDisconnectStations() { return disconnectStations; }
        
        @Override
        public String toString() {
            return String.format(
                "StationStatistics{total=%d, free=%d, work=%d, on=%d, off=%d, repair=%d, disconnect=%d}", 
                totalStations, freeStations, workStations, onStations, 
                offStations, repairStations, disconnectStations
            );
        }
    }
}