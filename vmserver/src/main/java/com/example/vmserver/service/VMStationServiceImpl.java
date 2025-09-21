package com.example.vmserver.service;

import com.example.vmserver.model.VMStation;
import com.example.vmserver.repository.VMStationRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VMStationServiceImpl implements VMStationService {
    private final VMStationRepository stationRepository;

    //Сохранение станции в БД
    @Override
    public VMStation createStation(VMStation station) {
        return stationRepository.save(station);
    }

    //Удаление станции
    @Override
    public void deleteStation(Long id) {
        VMStation station = stationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Станция не найден по идентификатору: " + id));
        stationRepository.delete(station);
    }

    //Обновление полей станции
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
    @Override
    public List<VMStation> getAllStations() {
        return stationRepository.findAll();
    }

    //Получить станцию по ID
    @Override
    public VMStation getStationById(Long id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Станция не найден по идентификатору: " + id));
    }
}
