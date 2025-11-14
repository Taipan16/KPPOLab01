package com.example.vmserver.controller;

import com.example.vmserver.dto.VMUserDTO;
import com.example.vmserver.model.VMUser;
import com.example.vmserver.service.VMUserService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class VMUserController {
    private final VMUserService userService;

    // Создание нового пользователя
    @PostMapping
    public ResponseEntity<VMUser> createUser(@RequestBody VMUser user) {
        VMUser createdUser = userService.createUser(user.getUsername(), user.getPassword());
        return ResponseEntity.ok(createdUser);
    }

    // Удаление пользователя по ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteVMUser(id);
        return ResponseEntity.ok().build();
    }

    // Обновление пользователя через DTO
    @PutMapping("/{id}")
    public ResponseEntity<VMUserDTO> updateUserDTO(
            @PathVariable Long id, 
            @RequestBody VMUserDTO userDTO) {
        VMUserDTO updatedUser = userService.updateVMUserDTO(id, userDTO);
        return ResponseEntity.ok(updatedUser);
    }

    // Получение списка всех пользователей в виде DTO
    @GetMapping
    public ResponseEntity<List<VMUserDTO>> getAllUsers() {
        List<VMUserDTO> users = userService.getVMUsers();
        return ResponseEntity.ok(users);
    }

    // Получение пользователя по ID в виде DTO
    @GetMapping("/{id}")
    public ResponseEntity<VMUserDTO> getUserById(@PathVariable Long id) {
        VMUserDTO user = userService.getVMUserDTO(id);
        return ResponseEntity.ok(user);
    }
    
    // Получение пользователя по имени пользователя
    @GetMapping("/username/{username}")
    public ResponseEntity<VMUserDTO> getUserByUsername(@PathVariable String username) {
        VMUserDTO user = userService.getVMUserDTOByUsername(username);
        return ResponseEntity.ok(user);
    }

    // Сброс пароля пользователя
    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> resetPassword(
            @PathVariable Long id,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        userService.resetPassword(id, oldPassword, newPassword);
        return ResponseEntity.ok().build();
    }

    // Сброс пароля пользователя по имени пользователя
    @PatchMapping("/username/{username}/password")
    public ResponseEntity<Void> resetPasswordByUsername(
            @PathVariable String username,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        userService.resetPassword(username, oldPassword, newPassword);
        return ResponseEntity.ok().build();
    }
}