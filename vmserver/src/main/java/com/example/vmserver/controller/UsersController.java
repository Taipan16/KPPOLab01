package com.example.vmserver.controller;

import org.springframework.web.bind.annotation.RestController;

import com.example.vmserver.model.User;
import com.example.vmserver.servise.UserService;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@RequestMapping("/API")
@RequiredArgsConstructor
public class UsersController {
    private final UserService userService;

    @PostMapping("/addUser")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        try{
            User createdUser = userService.createUser(user);
            return ResponseEntity.ok(createdUser);
        }
        catch(Exception ex){
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/getUser/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        try{
            Optional<User> user = userService.getUserById(id);
                return user.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        }
        catch(Exception ex){
            return ResponseEntity.badRequest().build();
        }
        
    }
    
    @GetMapping("/getUsers")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        try{
            Optional<User> user = userService.getUserById(id);
            if (user.isPresent()) {
                User existingUser = user.get();
                existingUser.setLogin(userDetails.getLogin());
                existingUser.setEmail(userDetails.getEmail());
                existingUser.setHashpassword(userDetails.getHashpassword());
                User updatedUser = userService.updateUser(existingUser);
                return ResponseEntity.ok(updatedUser);
            }
            return ResponseEntity.notFound().build();
        }
        catch(Exception ex){
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try{
            if (userService.getUserById(id).isPresent()) {
                userService.deleteUser(id);
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.notFound().build();
        }
        catch(Exception ex){
            return ResponseEntity.badRequest().build();
        }
    }

}
