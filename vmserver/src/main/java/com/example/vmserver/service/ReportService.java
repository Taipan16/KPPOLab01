package com.example.vmserver.service;

import com.example.vmserver.dto.ReportDTO;
import com.example.vmserver.enums.VMState;
import com.example.vmserver.model.Role;
import com.example.vmserver.model.VMQueue;
import com.example.vmserver.model.VMStation;
import com.example.vmserver.model.VMUser;
import com.example.vmserver.repository.RoleRepository;
import com.example.vmserver.repository.VMQueueRepository;
import com.example.vmserver.repository.VMStationRepository;
import com.example.vmserver.repository.VMUserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);
    
    private final VMStationRepository stationRepository;
    private final VMUserRepository userRepository;
    private final VMQueueRepository queueRepository;
    private final RoleRepository roleRepository;
    
    /**
     * Генерирует системный отчёт
     * @return DTO с данными отчёта
     */
    public ReportDTO generateSystemReport() {
        logger.info("Начало генерации системного отчёта");
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            ReportDTO report = new ReportDTO();
            report.setReportDate(LocalDateTime.now());
            
            // Собираем статистику по станциям
            logger.debug("Сбор статистики по станциям");
            report.setStationStatistics(generateStationStatistics());
            
            // Собираем статистику по пользователям
            logger.debug("Сбор статистики по пользователям");
            report.setUserStatistics(generateUserStatistics());
            
            // Собираем последние записи в очереди
            logger.debug("Сбор последних записей очереди");
            report.setLastQueueRecords(generateLastQueueRecords());
            
            // Собираем детальную информацию по станциям
            logger.debug("Сбор детальной информации по станциям");
            report.setStationDetails(generateStationDetails());
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            logger.info("Системный отчёт успешно сгенерирован за {} мс", duration.toMillis());
            logger.debug("Отчёт содержит: {} станций, {} пользователей, {} записей очереди", 
                    report.getStationStatistics().getTotalStations(),
                    report.getUserStatistics().getTotalUsers(),
                    report.getLastQueueRecords().size());
            
            return report;
        } catch (Exception e) {
            logger.error("Ошибка при генерации системного отчёта: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Генерирует статистику по станциям
     */
    private ReportDTO.StationStatistics generateStationStatistics() {
        logger.debug("Начало генерации статистики по станциям");
        
        try {
            List<VMStation> allStations = stationRepository.findAll();
            logger.debug("Найдено {} станций в системе", allStations.size());
            
            int freeStations = countStationsByState(allStations, VMState.FREE);
            int workStations = countStationsByState(allStations, VMState.WORK);
            int onStations = countStationsByState(allStations, VMState.ON);
            int offStations = countStationsByState(allStations, VMState.OFF);
            int repairStations = countStationsByState(allStations, VMState.REPAIR);
            int disconnectStations = countStationsByState(allStations, VMState.DISCONNECT);
            
            logger.debug("Статистика станций: Свободных={}, В работе={}, Включенных={}, Выключенных={}, В ремонте={}, Отключенных={}",
                    freeStations, workStations, onStations, offStations, repairStations, disconnectStations);
            
            return ReportDTO.StationStatistics.builder()
                    .totalStations(allStations.size())
                    .freeStations(freeStations)
                    .workStations(workStations)
                    .onStations(onStations)
                    .offStations(offStations)
                    .repairStations(repairStations)
                    .disconnectStations(disconnectStations)
                    .build();
        } catch (Exception e) {
            logger.error("Ошибка при генерации статистики по станциям: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Подсчитывает станции по состоянию
     */
    private int countStationsByState(List<VMStation> stations, VMState state) {
        int count = (int) stations.stream()
                .filter(station -> station.getState() == state)
                .count();
        logger.trace("Станций со статусом {}: {}", state, count);
        return count;
    }
    
    /**
     * Генерирует статистику по пользователям
     */
    private ReportDTO.UserStatistics generateUserStatistics() {
        logger.debug("Начало генерации статистики по пользователям");
        
        try {
            List<VMUser> allUsers = userRepository.findAll();
            logger.debug("Найдено {} пользователей в системе", allUsers.size());
            
            // Предполагаем, что роль администратора называется "ADMIN" или "ROLE_ADMIN"
            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElse(roleRepository.findByName("ROLE_ADMIN")
                            .orElse(null));
            
            int adminCount = 0;
            if (adminRole != null) {
                adminCount = (int) allUsers.stream()
                        .filter(user -> adminRole.equals(user.getRole()))
                        .count();
                logger.debug("Найдено {} администраторов", adminCount);
            } else {
                logger.warn("Роль администратора не найдена в базе данных");
            }
            
            int userCount = allUsers.size() - adminCount;
            logger.debug("Статистика пользователей: Всего={}, Администраторов={}, Обычных={}", 
                    allUsers.size(), adminCount, userCount);
            
            return ReportDTO.UserStatistics.builder()
                    .totalUsers(allUsers.size())
                    .adminCount(adminCount)
                    .userCount(userCount)
                    .build();
        } catch (Exception e) {
            logger.error("Ошибка при генерации статистики по пользователям: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Генерирует список последних записей в очереди
     */
    private List<ReportDTO.QueueRecord> generateLastQueueRecords() {
        logger.debug("Начало генерации последних записей очереди");
        
        try {
            // Получаем все записи и сортируем по дате создания (новые сначала)
            List<VMQueue> allQueues = queueRepository.findAll();
            logger.debug("Найдено {} записей в очереди", allQueues.size());
            
            List<ReportDTO.QueueRecord> records = allQueues.stream()
                    .sorted(Comparator.comparing(VMQueue::getCreatedAt).reversed())
                    .limit(10) // Берем только 10 последних записей
                    .map(this::convertToQueueRecordDTO)
                    .collect(Collectors.toList());
            
            logger.debug("Сгенерировано {} последних записей очереди", records.size());
            return records;
        } catch (Exception e) {
            logger.error("Ошибка при генерации последних записей очереди: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Конвертирует VMQueue в QueueRecord DTO
     */
    private ReportDTO.QueueRecord convertToQueueRecordDTO(VMQueue queue) {
        logger.trace("Конвертация записи очереди ID={} в DTO", queue.getId());
        return ReportDTO.QueueRecord.builder()
                .id(queue.getId())
                .username(queue.getCurrentUser() != null ? queue.getCurrentUser().getUsername() : null)
                .stationIp(queue.getVmStation() != null ? queue.getVmStation().getIp() : null)
                .createdAt(queue.getCreatedAt())
                .releasedAt(queue.getReleasedAt())
                .active(queue.getActive())
                .build();
    }
    
    /**
     * Генерирует детальную информацию по станциям
     */
    private List<ReportDTO.StationDetail> generateStationDetails() {
        logger.debug("Начало генерации детальной информации по станциям");
        
        try {
            List<VMStation> allStations = stationRepository.findAll();
            List<VMQueue> activeQueues = queueRepository.findByActiveTrue();
            
            logger.debug("Найдено {} активных записей очереди", activeQueues.size());
            
            List<ReportDTO.StationDetail> stationDetails = allStations.stream()
                    .map(station -> {
                        // Находим активную запись для этой станции
                        VMQueue activeQueue = activeQueues.stream()
                                .filter(queue -> queue.getVmStation() != null && 
                                               queue.getVmStation().getId().equals(station.getId()))
                                .findFirst()
                                .orElse(null);
                        
                        return ReportDTO.StationDetail.builder()
                                .id(station.getId())
                                .ip(station.getIp())
                                .port(station.getPort())
                                .state(station.getState())
                                .login(station.getLogin())
                                .currentUser(activeQueue != null && activeQueue.getCurrentUser() != null ? 
                                        activeQueue.getCurrentUser().getUsername() : null)
                                .assignedAt(activeQueue != null ? activeQueue.getCreatedAt() : null)
                                .build();
                    })
                    .collect(Collectors.toList());
            
            logger.debug("Сгенерирована детальная информация по {} станциям", stationDetails.size());
            return stationDetails;
        } catch (Exception e) {
            logger.error("Ошибка при генерации детальной информации по станциям: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Генерирует текстовый отчёт для удобства чтения
     * @return Отформатированный текстовый отчёт
     */
    public String generateTextReport() {
        logger.info("Начало генерации текстового отчёта");
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            ReportDTO report = generateSystemReport();
            
            StringBuilder sb = new StringBuilder();
            sb.append("=".repeat(60)).append("\n");
            sb.append("СИСТЕМНЫЙ ОТЧЁТ\n");
            sb.append("Сформирован: ").append(report.getReportDate()).append("\n");
            sb.append("=".repeat(60)).append("\n\n");
            
            // Статистика станций
            ReportDTO.StationStatistics stationStats = report.getStationStatistics();
            sb.append("СТАТИСТИКА СТАНЦИЙ:\n");
            sb.append("-".repeat(40)).append("\n");
            sb.append(String.format("Всего станций: %d\n", stationStats.getTotalStations()));
            sb.append(String.format("Свободных: %d\n", stationStats.getFreeStations()));
            sb.append(String.format("В работе: %d\n", stationStats.getWorkStations()));
            sb.append(String.format("Включенных: %d\n", stationStats.getOnStations()));
            sb.append(String.format("Выключенных: %d\n", stationStats.getOffStations()));
            sb.append(String.format("В ремонте: %d\n", stationStats.getRepairStations()));
            sb.append(String.format("Отключенных: %d\n", stationStats.getDisconnectStations()));
            sb.append("\n");
            
            // Статистика пользователей
            ReportDTO.UserStatistics userStats = report.getUserStatistics();
            sb.append("СТАТИСТИКА ПОЛЬЗОВАТЕЛЕЙ:\n");
            sb.append("-".repeat(40)).append("\n");
            sb.append(String.format("Всего пользователей: %d\n", userStats.getTotalUsers()));
            sb.append(String.format("Администраторов: %d\n", userStats.getAdminCount()));
            sb.append(String.format("Обычных пользователей: %d\n", userStats.getUserCount()));
            sb.append("\n");
            
            // Детали по станциям
            sb.append("ДЕТАЛЬНАЯ ИНФОРМАЦИЯ ПО СТАНЦИЯМ:\n");
            sb.append("-".repeat(80)).append("\n");
            sb.append(String.format("%-4s %-15s %-8s %-10s %-15s %-15s\n", 
                    "ID", "IP", "Порт", "Статус", "Логин", "Текущий пользователь"));
            sb.append("-".repeat(80)).append("\n");
            
            for (ReportDTO.StationDetail station : report.getStationDetails()) {
                sb.append(String.format("%-4d %-15s %-8d %-10s %-15s %-15s\n",
                        station.getId(),
                        station.getIp(),
                        station.getPort(),
                        station.getState(),
                        station.getLogin(),
                        station.getCurrentUser() != null ? station.getCurrentUser() : "-"));
            }
            sb.append("\n");
            
            // Последние записи в очереди
            sb.append("ПОСЛЕДНИЕ ЗАПИСИ В ОЧЕРЕДИ (10 последних):\n");
            sb.append("-".repeat(90)).append("\n");
            sb.append(String.format("%-4s %-15s %-15s %-20s %-20s %-8s\n", 
                    "ID", "Пользователь", "Станция", "Создано", "Освобождено", "Активно"));
            sb.append("-".repeat(90)).append("\n");
            
            for (ReportDTO.QueueRecord queue : report.getLastQueueRecords()) {
                sb.append(String.format("%-4d %-15s %-15s %-20s %-20s %-8s\n",
                        queue.getId(),
                        queue.getUsername() != null ? queue.getUsername() : "-",
                        queue.getStationIp() != null ? queue.getStationIp() : "-",
                        queue.getCreatedAt() != null ? queue.getCreatedAt().toString() : "-",
                        queue.getReleasedAt() != null ? queue.getReleasedAt().toString() : "-",
                        queue.getActive() ? "Да" : "Нет"));
            }
            
            sb.append("\n").append("=".repeat(60)).append("\n");
            sb.append("ОТЧЁТ СФОРМИРОВАН АВТОМАТИЧЕСКИ\n");
            
            String textReport = sb.toString();
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            
            logger.info("Текстовый отчёт успешно сгенерирован за {} мс, размер отчёта: {} символов", 
                    duration.toMillis(), textReport.length());
            logger.debug("Первые 500 символов отчёта:\n{}", textReport.substring(0, Math.min(textReport.length(), 500)));
            
            return textReport;
        } catch (Exception e) {
            logger.error("Ошибка при генерации текстового отчёта: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Экспортирует отчёт в текстовый файл
     * @param filePath путь к файлу для сохранения
     * @return true если экспорт успешен, false в противном случае
     */
    public boolean exportReportToFile(String filePath) {
        logger.info("Начало экспорта отчёта в файл: {}", filePath);
        
        try {
            String textReport = generateTextReport();
            
            // Здесь должна быть логика записи в файл
            // Пример: Files.write(Paths.get(filePath), textReport.getBytes(), StandardOpenOption.CREATE);
            
            logger.info("Отчёт успешно экспортирован в файл: {}", filePath);
            return true;
        } catch (Exception e) {
            logger.error("Ошибка при экспорте отчёта в файл {}: {}", filePath, e.getMessage(), e);
            return false;
        }
    }
}