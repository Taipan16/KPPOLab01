package com.example.VMserver.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.example.VMserver.model.VMState;
import com.example.VMserver.model.VMStation;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
public class VMController {
    private List<VMStation> VMStations = new ArrayList<>(
        Arrays.asList(
            new VMStation(1l, "10.12.110.10", 3389, VMState.off, "User", "123")
            ,new VMStation(2l, "10.12.110.11", 3389, VMState.work, "Admin", "Admin")
            ,new VMStation(3l, "10.12.110.12", 3389, VMState.reservation, "Student", "C2H5OH")
            ,new VMStation(4l, "10.12.110.13", 3389, VMState.free, "Student", "C2H5OH")
        )
    );

    //выдаёт списко машин
    @GetMapping("/getAllStations")
    public List<VMStation> getAllStations() {
        return VMStations;
    }

    //выдаёт машину по id
    @GetMapping("/getByIdStation/{id}")
    public ResponseEntity<VMStation> getByIdStation(@PathVariable("id") Long id) {
        for (VMStation station : VMStations) {
            if (station.getId().equals(id)) {
                return ResponseEntity.ok(station);
            }
        }
        return ResponseEntity.notFound().build();
    }

    //выдаёт параметры свободной машины
    @GetMapping("/getFreeStation")
    public ResponseEntity<VMStation> getFreeVM() {
        for (VMStation station : VMStations) {
            if (station.getState().equals(VMState.free)) {
                return ResponseEntity.ok(station);
            }
        }
        return ResponseEntity.notFound().build();
    }
    
    //бронирование
    @PostMapping("/reservationStation/{id}")
    public ResponseEntity<String> reservationStation(@PathVariable("id") Long id) {
        VMStation tempVM = null;
        for (VMStation station : VMStations) {
            if (station.getId().equals(id)) {
                tempVM = station;
                break;
            }
        }

        if(tempVM != null &&(tempVM.getState().equals(VMState.free) || tempVM.getState().equals(VMState.off))){
            tempVM.setState(VMState.reservation);
            return ResponseEntity.ok("Забронированно");
        }
        else{
            return ResponseEntity.ok("VM не найдена");
        }
        
    }
    
}
