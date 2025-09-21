package com.example.vmserver.service;

import com.example.vmserver.model.VMStation;
import java.util.List;

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

    //Показать станцию по IP
    //VMStation getStationByIp(String ip);
}
