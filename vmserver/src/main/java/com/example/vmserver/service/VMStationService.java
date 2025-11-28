package com.example.vmserver.service;

import com.example.vmserver.model.VMStation;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VMStationService {
    //Добавить станцию
    VMStation createStation(VMStation station);

    //Удалить станцию
    void deleteStation(Long id);

    //Обновить станцию
    VMStation updateStation(Long id, VMStation stationDetails);

    //Показать все станции
    List<VMStation> getAllStations();
    
    //Показать станцию по ID
    VMStation getStationById(Long id);

    Page<VMStation> getByFilter(String login, Integer min, Integer max, Pageable pageable);

    //выгрузка данных
    String exportStationsToCsv();

    //Показать станцию по IP
    //VMStation getStationByIp(String ip);
}
