package com.example.vmserver.controller;

import com.example.vmserver.dto.CreateUserRequest;
import com.example.vmserver.dto.PasswordResetRequest;
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
    public ResponseEntity<VMUserDTO> createUser(@RequestBody CreateUserRequest request) {
        VMUserDTO createdUser = userService.createUser(request.username(), request.password());
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
    
    // Получение пользователя
    @GetMapping("/username/{username}")
    public ResponseEntity<VMUserDTO> getUserByUsername(@PathVariable String username) {
        VMUserDTO user = userService.getVMUserDTOByUsername(username);
        return ResponseEntity.ok(user);
    }

    // Сброс пароля пользователя по имени пользователя
    @PatchMapping("/username/{username}/password")
    public ResponseEntity<Void> resetPasswordByUsername(
            @PathVariable String username,
            @RequestBody PasswordResetRequest request) {
        userService.resetPassword(username, request.oldPassword(), request.newPassword());
        return ResponseEntity.ok().build();
    }
}