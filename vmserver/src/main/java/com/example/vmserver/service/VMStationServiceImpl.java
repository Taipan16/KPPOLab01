package com.example.vmserver.service;

import com.example.vmserver.enums.VMState;
import com.example.vmserver.model.VMStation;
import com.example.vmserver.repository.VMStationRepository;
import com.example.vmserver.specifications.VMStationSpecifications;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VMStationServiceImpl implements VMStationService {
    private final VMStationRepository stationRepository;

    //Сохранение станции в БД
    @Transactional
    @CacheEvict(value = "VMStation", allEntries = true)
    @Override
    public VMStation createStation(VMStation station) {
        return stationRepository.save(station);
    }

    //Удаление станции
    @Transactional
    @CacheEvict(value = "VMStation", allEntries = true)
    @Override
    public void deleteStation(Long id) {
        VMStation station = stationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Станция не найден по идентификатору: " + id));
        stationRepository.delete(station);
    }

    //Обновление полей станции
    @Transactional
    @CacheEvict(value = "VMStation", allEntries = true)
    @Override
    public VMStation updateStation(Long id, VMStation stationDetails) {
        VMStation station = stationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Станция не найден по идентификатору: " + id));
        
        station.setIp(stationDetails.getIp());
        station.setPort(stationDetails.getPort());
        station.setState(stationDetails.getState());
        station.setLogin(stationDetails.getLogin());
        station.setHashPassword(stationDetails.getHashPassword());
        
        return stationRepository.save(station);
    }

    //Получение всех станций
    @Transactional
    @Cacheable(value = "VMStations")
    @Override
    public List<VMStation> getAllStations() {
        return stationRepository.findAll();
    }

    //Получить станцию по ID
    @Transactional
    @Cacheable(value = "VMStation", key = "#id")
    @Override
    public VMStation getStationById(Long id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Станция не найден по идентификатору: " + id));
    }
    
    @Override
    public Page<VMStation> getByFilter(String login, Integer min, Integer max, Pageable pageable){
        return stationRepository.findAll(VMStationSpecifications.filter(login, min, max), pageable);
    }

    
    @Override
    public String exportStationsToCsv() {
    List<VMStation> stations = stationRepository.findAll();
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
    
    return csvBuilder.toString();
    }

    @Transactional
    @CacheEvict(value = {"VMStation", "VMStations"}, allEntries = true)
    @Override
    public String importStationsFromCsv(MultipartFile file) {
        List<VMStation> importedStations = new ArrayList<>();
        int importedCount = 0;
        int skippedCount = 0;
        
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isFirstLine = true;
            
            while ((line = br.readLine()) != null) {
                // Пропускаем заголовок
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                
                String[] data = line.split(",");
                if (data.length < 6) {
                    skippedCount++;
                    continue; // Пропускаем некорректные строки
                }
                
                try {
                    VMStation station = new VMStation();
                    
                    // ID игнорируем при импорте, т.к. он генерируется автоматически
                    // data[0] - ID
                    station.setIp(data[1].trim());
                    station.setPort(Integer.parseInt(data[2].trim()));
                    station.setState(VMState.valueOf(data[3].trim()));
                    station.setLogin(data[4].trim());
                    station.setHashPassword(data[5].trim());
                    
                    // Проверяем, существует ли станция с таким IP
                    VMStation existingStation = stationRepository.findByIp(station.getIp());
                    if (existingStation != null) {
                        // Обновляем существующую станцию
                        existingStation.setPort(station.getPort());
                        existingStation.setState(station.getState());
                        existingStation.setLogin(station.getLogin());
                        existingStation.setHashPassword(station.getHashPassword());
                        stationRepository.save(existingStation);
                    } else {
                        // Создаем новую станцию
                        stationRepository.save(station);
                    }
                    
                    importedCount++;
                } catch (Exception e) {
                    skippedCount++;
                    // Логирование ошибки (можно добавить логгер)
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при импорте CSV файла: " + e.getMessage(), e);
        }
        
        return String.format("Импорт завершен. Успешно импортировано: %d, Пропущено: %d", 
                           importedCount, skippedCount);
    }

}
