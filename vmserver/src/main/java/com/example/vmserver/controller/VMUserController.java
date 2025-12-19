package com.example.vmserver.controller;

import com.example.vmserver.dto.CreateUserRequest;
import com.example.vmserver.dto.PasswordResetRequest;
import com.example.vmserver.dto.VMUserDTO;
import com.example.vmserver.model.VMUser;
import com.example.vmserver.service.VMUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Контроллер пользователей", description = "API для управления пользователями")
public class VMUserController {
    private final VMUserService userService;

    @PostMapping
    @Operation(summary = "Создание нового пользователя", 
               description = "Создает нового пользователя с заданными параметрами")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Пользователь успешно создан")
    })
    public ResponseEntity<VMUserDTO> createUser(@RequestBody CreateUserRequest request) {
        VMUserDTO createdUser = userService.createUser(request.username(), request.password());
        return ResponseEntity.ok(createdUser);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удаление пользователя по ID", 
               description = "Удаляет пользователя с указанным идентификатором")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Пользователь успешно удален")
    })
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID пользователя", required = true, example = "1")
            @PathVariable Long id) {
        userService.deleteVMUser(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновление данных пользователя", 
               description = "Обновляет информацию о пользователе по указанному ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Данные пользователя успешно обновлены")
    })
    public ResponseEntity<VMUserDTO> updateUserDTO(
            @Parameter(description = "ID пользователя", required = true, example = "2")
            @PathVariable Long id,
            @Parameter(description = "Обновленные данные пользователя", required = true)
            @RequestBody VMUserDTO userDTO) {
        VMUserDTO updatedUser = userService.updateVMUserDTO(id, userDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping
    @Operation(summary = "Получение списка всех пользователей", 
               description = "Возвращает список всех зарегистрированных пользователей")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список пользователей успешно получен")
    })
    public ResponseEntity<List<VMUserDTO>> getAllUsers() {
        List<VMUserDTO> users = userService.getVMUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получение пользователя по ID", 
               description = "Возвращает данные пользователя по указанному идентификатору")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Пользователь найден")
    })
    public ResponseEntity<VMUserDTO> getUserById(
            @Parameter(description = "ID пользователя", required = true, example = "3")
            @PathVariable Long id) {
        VMUserDTO user = userService.getVMUserDTO(id);
        return ResponseEntity.ok(user);
    }
    
    @GetMapping("/username/{username}")
    @Operation(summary = "Получение пользователя по имени", 
               description = "Возвращает данные пользователя по имени пользователя (username)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Пользователь найден")
    })
    public ResponseEntity<VMUserDTO> getUserByUsername(
            @Parameter(description = "Имя пользователя", required = true, example = "user")
            @PathVariable String username) {
        VMUserDTO user = userService.getVMUserDTOByUsername(username);
        return ResponseEntity.ok(user);
    }

    @PatchMapping("/username/{username}/password")
    @Operation(summary = "Сброс пароля пользователя", 
               description = "Изменяет пароль пользователя по его имени. Требуется старый пароль для подтверждения.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Пароль успешно изменен")
    })
    public ResponseEntity<Void> resetPasswordByUsername(
            @Parameter(description = "Имя пользователя", required = true, example = "user")
            @PathVariable String username,
            @Parameter(description = "Данные для сброса пароля", required = true)
            @RequestBody PasswordResetRequest request) {
        userService.resetPassword(username, request.oldPassword(), request.newPassword());
        return ResponseEntity.ok().build();
    }
}