package com.example.vmserver.controller;

import com.example.vmserver.model.VMStation;
import com.example.vmserver.service.VMStationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
public class VMStationController {
    private final VMStationService stationService;

    //Создание новой станции
    @PostMapping
    public ResponseEntity<VMStation> createStation(@RequestBody VMStation station) {
        return ResponseEntity.ok(stationService.createStation(station));
    }

    //Удаление станции по ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStation(@PathVariable Long id) {
        stationService.deleteStation(id);
        return ResponseEntity.ok().build();
    }

    //Обновление станции
    @PutMapping("/{id}")
    public ResponseEntity<VMStation> updateStation(
            @PathVariable Long id, 
            @RequestBody VMStation stationDetails) {
        return ResponseEntity.ok(stationService.updateStation(id, stationDetails));
    }

    //Получение списка всех станции
    @GetMapping
    public ResponseEntity<List<VMStation>> getAllStations() {
        return ResponseEntity.ok(stationService.getAllStations());
    }

    //Получение станции по ID
    @GetMapping("/{id}")
    public ResponseEntity<VMStation> getStationById(@PathVariable Long id) {
        return ResponseEntity.ok(stationService.getStationById(id));
    }
}