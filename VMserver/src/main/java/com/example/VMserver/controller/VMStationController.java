package com.example.VMserver.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.example.VMserver.entity.eVMState;
import com.example.VMserver.model.VMStation;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
public class VMStationController {
    private List<VMStation> VMStations = new ArrayList<>(
        Arrays.asList(
            new VMStation(1l, "10.12.110.10", 3389, eVMState.off, "User", "123")
            ,new VMStation(2l, "10.12.110.11", 3389, eVMState.on, "Admin", "Admin")
            ,new VMStation(3l, "10.12.110.12", 3389, eVMState.repair, "Student", "C2H5OH")
            ,new VMStation(4l, "10.12.110.13", 3389, eVMState.on, "Student", "C2H5OH")
        )
    );
    private long lastIndex = 5;
    static private Pattern IPV4_PATTERN = Pattern.compile("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

    //выдаёт список машин
    @GetMapping("/getAllStations")
    public List<VMStation> getAllStations() {
            return VMStations;
    }

    //выдаёт машину по id
    @GetMapping("/getByIdStation/{id}")
    public ResponseEntity<VMStation> getByIdStation(@PathVariable("id") Long id) {
        try{
            for (VMStation station : VMStations) {
            if (station.getId().equals(id)) {
                return ResponseEntity.ok(station);
            }
        }
        return ResponseEntity.notFound().build();
        }
        catch(Exception ex){
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        
    }

    //выдаёт параметры включенной машины
    @GetMapping("/getAllOnStation")
    public ResponseEntity<List<VMStation>> getAllOnVM() {
        List<VMStation> onStation = new ArrayList<>();;
        try{
            for (VMStation station : VMStations) {
            if (station.getState().equals(eVMState.on)) {
                onStation.add(station);
            }
        }
        return new ResponseEntity<>(onStation, HttpStatus.OK);
        }
        catch(Exception ex){
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    //выдаёт параметры выключенной машины
    @GetMapping("/getAllOffStation")
    public ResponseEntity<List<VMStation>> getAllOffVM() {
        List<VMStation> onStation = new ArrayList<>();;
        try{
            for (VMStation station : VMStations) {
            if (station.getState().equals(eVMState.off)) {
                onStation.add(station);
            }
        }
        return new ResponseEntity<>(onStation, HttpStatus.OK);
        }
        catch(Exception ex){
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    //выдаёт параметры машины
    @GetMapping("/getAllRepairStation")
    public ResponseEntity<List<VMStation>> getAllRepairVM() {
        List<VMStation> onStation = new ArrayList<>();;
        try{
            for (VMStation station : VMStations) {
            if (station.getState().equals(eVMState.repair)) {
                onStation.add(station);
            }
        }
        return new ResponseEntity<>(onStation, HttpStatus.OK);
        }
        catch(Exception ex){
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    //отключить выбранную машину
    @PostMapping("/offVM/{id}")
    public ResponseEntity<String> offVM(@PathVariable("id") Long id) {
        try{
            for (VMStation station : VMStations) {
            if (station.getId().equals(id)) {
                if(station.getState() == eVMState.off){
                    return new ResponseEntity<>("Внимание VM оключена", HttpStatus.OK);
                }
                else{
                    station.setState(eVMState.off);
                    return new ResponseEntity<>("VM оключена", HttpStatus.OK);
                }
            }
        }
        }
        catch (Exception ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>("VM не найдена", HttpStatus.OK);
    }
    
    //включить выбранную машину
    @PostMapping("/onVM/{id}")
    public ResponseEntity<String> onVM(@PathVariable("id") Long id) {
        try{
            for (VMStation station : VMStations) {
            if (station.getId().equals(id)) {
                if(station.getState() == eVMState.on || station.getState() == eVMState.repair){
                    return new ResponseEntity<>("Внимание VM в работе", HttpStatus.OK);
                }
                else{
                    station.setState(eVMState.on);
                    return new ResponseEntity<>("VM оключена", HttpStatus.OK);
                }
            }
        }
        }
        catch (Exception ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>("VM не найдена", HttpStatus.OK);
    }
    
    //переписал код из C#, хз но работает :)
    //добавление ВМ
    @PostMapping("/addVMStation/")
    public ResponseEntity<String> addVMStation(@Valid @RequestBody VMStation request) {
        try{
            //проверяем ip
            if (!IPV4_PATTERN.matcher(request.getIp()).matches()) {
                return ResponseEntity.badRequest().body("Неверный формат IP");
            }
            //создаём сущность
            VMStation station = new VMStation();
            station.setId(lastIndex);
            station.setIp(request.getIp());
            station.setPort(request.getPort());
            station.setState(eVMState.off);
            station.setLogin(request.getLogin());
            station.setHashPassword(request.getHashPassword());
            //добавляем сущность
            VMStations.add(station);
            lastIndex++;
            return ResponseEntity.ok("VMStation успешно добавлена");
        }
        catch(Exception ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } 
    }
    
    //Удаление ВМ
    @PostMapping("/deleteVMStationById/{id}")
    public ResponseEntity<String> postMethodName(@PathVariable("id") long id) {
        try{
            for (VMStation station : VMStations) {
            if (station.getId().equals(id)) {
                VMStations.remove(station);
                return ResponseEntity.ok("Удалена!");
            }
        }
        return ResponseEntity.ok("VM не найдена");
        }
        catch(Exception ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
        
    }
    
    /*
    //бронирование
    @PostMapping("/reservStation/{id}")
    public ResponseEntity<String> reservStation(@PathVariable("id") Long id) {
        try{
            VMStation tempVM = null;
            for (VMStation station : VMStations) {
                if (station.getId().equals(id)) {
                    tempVM = station;
                    break;
                }
            }

            if(tempVM != null &&(tempVM.getState().equals(eVMState.free) || tempVM.getState().equals(eVMState.off))){
                tempVM.setState(eVMState.reservation);
                return ResponseEntity.ok("Забронированно");
            }
            else{
                return ResponseEntity.ok("VM не найдена");
            }
        }
        catch(Exception ex)
        {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }
    */

    /*
    //выдаёт и подлкючает машину
    @GetMapping("/getFreeConnect")
    public ResponseEntity<VMStation> getFreeConnect() {
        try{
            for (VMStation station : VMStations) {
            if (station.getState().equals(VMState.free)) {
                station.setState(VMState.work);
                return ResponseEntity.ok(station);
            }
        }
        return ResponseEntity.notFound().build();
        }
        catch(Exception ex){
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }
    */
    
    /*
    //Освободить машину
    @PostMapping("/disconnectUserVM/{id}")
    public ResponseEntity<String> disconnectUserVM(@PathVariable("id") Long id) {
        try{
            for (VMStation station : VMStations) {
            if (station.getId().equals(id)) {
                if(station.getState() == VMState.work){
                    station.setState(VMState.free);
                    return new ResponseEntity<>("Машина освобождена", HttpStatus.OK);
                }
                else{
                    return new ResponseEntity<>("VM уже свободна", HttpStatus.OK);
                }
            }
        }
        }
        catch (Exception ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>("VM не найдена", HttpStatus.OK);
    }
    */
}
