package com.example.VMserver.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.example.VMserver.model.User;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
public class VMQueueController {
    private List<User> Users = new ArrayList<>(
        Arrays.asList(
            new User(1l, "test@mail.ru", "test@mail.ru", "judfshghsdfguhdsfiughidfsuhgiu")
            ,new User(2l, "test@mail.ru", "test@mail.ru", "judfshghsdfguhdsfiughidfsuhgiu")
            ,new User(3l, "test@mail.ru", "test@mail.ru", "judfshghsdfguhdsfiughidfsuhgiu")
        )
    );
    private long lastIndex = 4;

    //выдаёт список машин
    @GetMapping("/getAllUsers")
    public List<User> getAllStations() {
            return Users;
    }

    //выдаёт пользователя по id
    @GetMapping("/getUserById/{id}")
    public ResponseEntity<User> getByIdStation(@PathVariable("id") Long id) {
        try{
            for (User user : Users) {
            if (user.getId().equals(id)) {
                return ResponseEntity.ok(user);
            }
        }
        return ResponseEntity.notFound().build();
        }
        catch(Exception ex){
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        
    }

    //добавление пользователя
    @PostMapping("/addUser/")
    public ResponseEntity<String> addVMStation(@Valid @RequestBody User request) {
        try{
            //создаём сущность
            User user = new User();
            user.setId(lastIndex);
            user.setEMail(request.getEMail());
            user.setLogin(request.getLogin());
            user.setHashPassword(request.getHashPassword());
            //добавляем сущность
            Users.add(user);
            lastIndex++;
            return ResponseEntity.ok("User добавлен");
        }
        catch(Exception ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } 
    }
    
    //Удаление ВМ
    @PostMapping("/deleteUserById/{id}")
    public ResponseEntity<String> postMethodName(@PathVariable("id") long id) {
        try{
            for (User user : Users) {
            if (user.getId().equals(id)) {
                Users.remove(user);
                return ResponseEntity.ok("Удален!");
            }
        }
        return ResponseEntity.ok("User не найден");
        }
        catch(Exception ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
        
    }
}
