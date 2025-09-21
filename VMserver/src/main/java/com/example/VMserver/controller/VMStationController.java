package com.example.vmserver.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

//import lombok.RequiredArgsConstructor;

@RestController
//@RequiredArgsConstructor
public class VMStationController {
    
    @GetMapping("/vmstations1")
    public String getUsers(@RequestParam String param) {
        return new String();
    }
    
    @PostMapping("/vmstations2")
    public String createUsers(@RequestBody String entity) {
        return entity;
    }
    
    @DeleteMapping("/vmstations3")
    public String deleteUsers(@RequestBody String entity) {
        return entity;
    }
}
